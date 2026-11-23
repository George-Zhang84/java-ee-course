package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.OrderQuery;
import com.zhj.exp4.dto.response.OrderResponse;
import com.zhj.exp4.entity.Order;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.entity.Review;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.OrderStatus;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.enums.WalletType;
import com.zhj.exp4.event.OrderPaidEvent;
import com.zhj.exp4.mapper.BargainOfferMapper;
import com.zhj.exp4.mapper.OrderMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.mapper.ReviewMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.NotificationService;
import com.zhj.exp4.service.OrderService;
import com.zhj.exp4.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现。
 *
 * <p>重点看三个方法：{@link #create(Long)}、{@link #pay(Long)}、{@link #cancelInternal(Order, String)}，
 * 它们集中体现了"商品状态 + 订单状态 + 资金"三者的一致性保障。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int BATCH_LIMIT = 50;

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final BargainOfferMapper offerMapper;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;

    // ------------------------------------------------------------------
    // 下单
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse create(Long productId) {
        Long buyerId = SecurityUtils.currentUserId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (product.getSellerId().equals(buyerId)) {
            throw BizException.of("不能购买自己发布的商品");
        }
        if (!ProductStatus.canBuy(product.getStatus())) {
            throw BizException.stateConflict("商品已下架或已被他人购买");
        }

        // 关键一步：原子抢占商品。两个并发请求只有一个能把 ON_SALE 改成 LOCKED
        int locked = productMapper.casStatus(productId,
                ProductStatus.ON_SALE.name(), ProductStatus.LOCKED.name());
        if (locked == 0) {
            throw BizException.stateConflict("手慢了，商品刚被其他人下单");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setAmount(product.getPrice());
        order.setOrigin("DIRECT");
        order.setStatus(OrderStatus.UNPAID.name());
        // 显式写创建时间：超时取消任务依赖 create_time，不能用数据库默认值（时区可能不一致）
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        notificationService.send(product.getSellerId(), "ORDER", "有新的订单",
                "你的商品「" + product.getTitle() + "」已被下单，订单号 " + order.getOrderNo(), order.getId());
        log.info("下单成功 orderNo={} productId={} buyerId={}", order.getOrderNo(), productId, buyerId);

        long timeout = appProperties.getOrder().getTimeoutMinutes();
        return assemble(order, Map.of(product.getId(), product), Map.of(), Set.of(),
                "请在 " + timeout + " 分钟内完成支付，超时订单将自动取消");
    }

    // ------------------------------------------------------------------
    // 支付
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long buyerId = SecurityUtils.currentUserId();
        Order order = requireOrder(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw BizException.ownershipDenied();
        }
        if (!OrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw BizException.stateConflict("订单不是待支付状态，无法支付");
        }

        BigDecimal amount = order.getAmount();

        // 1) 扣买家余额：条件更新，余额不足直接抛异常，后续操作全部回滚
        walletService.deduct(buyerId, amount, WalletType.PAY, "ORDER", orderId,
                "支付订单 " + order.getOrderNo());

        // 2) 订单状态 CAS：若并发下已被取消，这里影响行数为 0，抛异常回滚上面的扣款
        int paid = orderMapper.markPaid(orderId, LocalDateTime.now());
        if (paid == 0) {
            throw BizException.stateConflict("订单状态已变更，请刷新后重试");
        }

        // 3) 卖家入账（平台内转账，无第三方支付）
        walletService.income(order.getSellerId(), amount, WalletType.INCOME, "ORDER", orderId,
                "订单 " + order.getOrderNo() + " 结算入账");

        // 4) 商品 LOCKED -> SOLD
        productMapper.casStatus(order.getProductId(),
                ProductStatus.LOCKED.name(), ProductStatus.SOLD.name());

        // 5) 发布领域事件（AFTER_COMMIT 监听器负责通知等旁路动作）
        eventPublisher.publishEvent(new OrderPaidEvent(this, orderId, order.getOrderNo(),
                order.getBuyerId(), order.getSellerId(), amount));
        log.info("订单支付成功 orderNo={} amount={}", order.getOrderNo(), amount);
    }

    // ------------------------------------------------------------------
    // 发货 / 收货 / 取消
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.currentUserId();
        if (!order.getSellerId().equals(currentUserId)) {
            throw BizException.ownershipDenied();
        }
        int rows = orderMapper.markShipped(orderId, LocalDateTime.now());
        if (rows == 0) {
            throw BizException.stateConflict("只有已支付的订单才能发货");
        }
        notificationService.send(order.getBuyerId(), "ORDER", "卖家已发货",
                "订单 " + order.getOrderNo() + " 已发货，请及时确认收货", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.currentUserId();
        if (!order.getBuyerId().equals(currentUserId)) {
            throw BizException.ownershipDenied();
        }
        int rows = orderMapper.markCompleted(orderId, LocalDateTime.now());
        if (rows == 0) {
            throw BizException.stateConflict("只有已发货的订单才能确认收货");
        }
        notificationService.send(order.getSellerId(), "ORDER", "买家已确认收货",
                "订单 " + order.getOrderNo() + " 已完成，欢迎评价", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.currentUserId();
        boolean isBuyer = order.getBuyerId().equals(currentUserId);
        boolean isSeller = order.getSellerId().equals(currentUserId);
        if (!isBuyer && !isSeller) {
            throw BizException.ownershipDenied();
        }
        if (!OrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw BizException.stateConflict("只有待支付订单可以取消");
        }
        cancelInternal(order, isBuyer ? "买家主动取消" : "卖家取消订单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBySystem(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.UNPAID.name().equals(order.getStatus())) {
            return;
        }
        cancelInternal(order, "超时未支付，系统自动取消");
        log.info("超时订单已自动取消 orderNo={}", order.getOrderNo());
    }

    /**
     * 取消订单的统一实现：订单置 CANCELLED、商品解锁、报价单作废、双方通知。
     *
     * <p>商品解锁用 CAS（LOCKED -> ON_SALE），如果商品已经被别的流程改过就不动，
     * 避免把"已售出"的商品错误地改回"在售"。
     */
    private void cancelInternal(Order order, String reason) {
        int rows = orderMapper.markCancelled(order.getId(), OrderStatus.UNPAID.name(), reason);
        if (rows == 0) {
            throw BizException.stateConflict("订单已被处理，请刷新");
        }
        productMapper.casStatus(order.getProductId(),
                ProductStatus.LOCKED.name(), ProductStatus.ON_SALE.name());

        // 议价成交的订单取消后，对应报价单作废，避免"已接受的报价"被再次使用
        if (order.getOfferId() != null) {
            offerMapper.expireAccepted(order.getOfferId());
        }

        notificationService.send(order.getBuyerId(), "ORDER", "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消：" + reason, order.getId());
        notificationService.send(order.getSellerId(), "ORDER", "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消：" + reason, order.getId());
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public PageResult<OrderResponse> page(OrderQuery query) {
        Long userId = SecurityUtils.currentUserId();
        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery(Order.class);
        if (query.isSellerView()) {
            wrapper.eq(Order::getSellerId, userId);
        } else {
            wrapper.eq(Order::getBuyerId, userId);
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Order::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = orderMapper.selectPage(Page.of(query.safePage(), query.safeSize()), wrapper);
        List<Order> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        Map<Long, Product> products = batchProducts(records.stream().map(Order::getProductId).toList());
        Map<Long, SysUser> users = batchUsers(records.stream()
                .flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId()))
                .toList());
        Set<Long> reviewed = reviewedOrderIds(records);

        List<OrderResponse> list = records.stream()
                .map(o -> assemble(o, products, users, reviewed, null))
                .toList();
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderResponse detail(Long orderId) {
        Order order = requireOrder(orderId);
        LoginUser loginUser = SecurityUtils.currentUser();
        boolean participant = order.getBuyerId().equals(loginUser.getUserId())
                || order.getSellerId().equals(loginUser.getUserId());
        if (!participant && !loginUser.hasRole("ADMIN") && !loginUser.hasRole("SUPER_ADMIN")) {
            throw BizException.ownershipDenied();
        }
        Map<Long, Product> products = batchProducts(List.of(order.getProductId()));
        Map<Long, SysUser> users = batchUsers(List.of(order.getBuyerId(), order.getSellerId()));
        return assemble(order, products, users, reviewedOrderIds(List.of(order)), null);
    }

    @Override
    public List<Long> findTimeoutOrderIds(int limit) {
        LocalDateTime deadline = LocalDateTime.now()
                .minusMinutes(appProperties.getOrder().getTimeoutMinutes());
        return orderMapper.selectTimeoutUnpaid(deadline, limit).stream().map(Order::getId).toList();
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private Order requireOrder(Long orderId) {
        Order order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 生成业务单号：ORD + 秒级时间 + 4 位随机数（唯一索引兜底，冲突概率极低） */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private Map<Long, Product> batchProducts(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, SysUser> batchUsers(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    /** 当前用户已评价过的订单 id 集合，用于前端隐藏"去评价"按钮 */
    private Set<Long> reviewedOrderIds(List<Order> orders) {
        Long userId;
        try {
            userId = SecurityUtils.currentUserId();
        } catch (Exception e) {
            return Set.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Set.of();
        }
        return reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
                        .in(Review::getOrderId, orderIds)
                        .eq(Review::getReviewerId, userId)).stream()
                .map(Review::getOrderId)
                .collect(Collectors.toSet());
    }

    /**
     * 组装响应并计算"当前用户可执行动作"。
     * 这里集中判断，前端不需要再写一遍状态机规则。
     */
    private OrderResponse assemble(Order order, Map<Long, Product> products,
                                   Map<Long, SysUser> users, Set<Long> reviewed,
                                   String tip) {
        Product product = products.get(order.getProductId());
        SysUser buyer = users.get(order.getBuyerId());
        SysUser seller = users.get(order.getSellerId());
        Long currentUserId = SecurityUtils.currentUserId();

        boolean isBuyer = order.getBuyerId().equals(currentUserId);
        boolean isSeller = order.getSellerId().equals(currentUserId);
        String status = order.getStatus();

        OrderResponse response = OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .productId(order.getProductId())
                .productTitle(product == null ? null : product.getTitle())
                .productCover(product == null ? null : product.getCoverUrl())
                .amount(order.getAmount())
                .origin(order.getOrigin())
                .originText("BARGAIN".equals(order.getOrigin()) ? "议价成交" : "直接购买")
                .offerId(order.getOfferId())
                .status(status)
                .statusText(statusText(status))
                .buyerId(order.getBuyerId())
                .buyerNickname(buyer == null ? null : buyer.getNickname())
                .sellerId(order.getSellerId())
                .sellerNickname(seller == null ? null : seller.getNickname())
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .shipTime(order.getShipTime())
                .finishTime(order.getFinishTime())
                .cancelReason(order.getCancelReason())
                .canPay(isBuyer && OrderStatus.UNPAID.name().equals(status))
                .canShip(isSeller && OrderStatus.PAID.name().equals(status))
                .canConfirm(isBuyer && OrderStatus.SHIPPED.name().equals(status))
                .canCancel(OrderStatus.UNPAID.name().equals(status) && (isBuyer || isSeller))
                .reviewed(reviewed.contains(order.getId()))
                .build();
        if (tip != null) {
            // 下单成功时给前端一句提示（含支付倒计时说明）
            log.debug("订单 {} 提示：{}", order.getOrderNo(), tip);
        }
        return response;
    }

    private String statusText(String status) {
        try {
            return OrderStatus.valueOf(status).getDescription();
        } catch (Exception e) {
            return status;
        }
    }
}
