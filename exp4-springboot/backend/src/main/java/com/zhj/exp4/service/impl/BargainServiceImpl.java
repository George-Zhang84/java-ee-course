package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.OfferCreateRequest;
import com.zhj.exp4.dto.response.OfferCreateResponse;
import com.zhj.exp4.dto.response.OfferResponse;
import com.zhj.exp4.entity.BargainOffer;
import com.zhj.exp4.entity.Order;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.OfferStatus;
import com.zhj.exp4.enums.OrderStatus;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.mapper.BargainOfferMapper;
import com.zhj.exp4.mapper.OrderMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.BargainService;
import com.zhj.exp4.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 议价服务实现。
 *
 * <p>{@link #accept(Long)} 是全项目并发风险最高的方法，处理顺序很关键：
 * <ol>
 *   <li>先把报价单 PENDING -> ACCEPTED（CAS），抢不到就说明已被处理或已过期；</li>
 *   <li>再把商品 ON_SALE -> LOCKED（CAS），抢不到说明商品刚被别人买走，抛异常整体回滚，
 *       报价单回到 PENDING 状态，不会出现"报价被接受但商品已卖出"的脏数据；</li>
 *   <li>最后生成订单，金额取报价金额并写入 offer_id（唯一索引防重复成交）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BargainServiceImpl implements BargainService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BargainOfferMapper offerMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfferCreateResponse createOffer(OfferCreateRequest request) {
        Long buyerId = SecurityUtils.currentUserId();
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (product.getSellerId().equals(buyerId)) {
            throw BizException.of("不能对自己的商品出价");
        }
        if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw BizException.stateConflict("商品已下架或已被他人购买");
        }
        if (!ProductStatus.canBargain(product.getStatus(), product.getAllowBargain())) {
            throw BizException.of("该商品不支持议价");
        }
        if (request.getOfferPrice().compareTo(product.getPrice()) >= 0) {
            throw BizException.of("出价必须低于售价 " + product.getPrice() + " 元");
        }
        if (offerMapper.countPending(product.getId(), buyerId) > 0) {
            throw BizException.of("你对该商品还有一条待处理的报价，请等待卖家回复");
        }
        int maxAttempts = appProperties.getOffer().getMaxAttempts();
        if (offerMapper.countTotal(product.getId(), buyerId) >= maxAttempts) {
            throw BizException.of("对同一商品最多出价 " + maxAttempts + " 次");
        }

        BargainOffer offer = new BargainOffer();
        offer.setProductId(product.getId());
        offer.setBuyerId(buyerId);
        offer.setSellerId(product.getSellerId());
        offer.setOfferPrice(request.getOfferPrice());
        offer.setMessage(request.getMessage());
        offer.setStatus(OfferStatus.PENDING.name());
        // 显式写入时间，避免依赖数据库默认值（数据库时区不一致时会差 8 小时）
        offer.setCreateTime(LocalDateTime.now());
        offer.setExpireTime(LocalDateTime.now().plusHours(appProperties.getOffer().getExpireHours()));
        offerMapper.insert(offer);

        // 达到卖家底价 -> 自动成交（底价为空则只能人工确认）
        if (product.getMinPrice() != null
                && request.getOfferPrice().compareTo(product.getMinPrice()) >= 0) {
            Long orderId = autoAccept(offer, product);
            log.info("议价自动成交 offerId={} orderId={} price={}", offer.getId(), orderId, request.getOfferPrice());
            return OfferCreateResponse.builder()
                    .offerId(offer.getId())
                    .autoAccepted(true)
                    .orderId(orderId)
                    .message("出价已达到卖家底价，已自动成交，请尽快支付")
                    .build();
        }

        notificationService.send(product.getSellerId(), "BARGAIN", "收到新的还价",
                "买家对「" + product.getTitle() + "」出价 " + request.getOfferPrice() + " 元", offer.getId());
        log.info("议价创建 offerId={} productId={} buyerId={} price={}",
                offer.getId(), product.getId(), buyerId, request.getOfferPrice());
        return OfferCreateResponse.builder()
                .offerId(offer.getId())
                .autoAccepted(false)
                .message("报价已发送，等待卖家回复")
                .build();
    }

    /**
     * 底价自动成交：与人工"接受报价"走同一套原子操作
     * （报价单 CAS → 商品 CAS → 生成订单），只是操作者从卖家变成了系统。
     */
    private Long autoAccept(BargainOffer offer, Product product) {
        int handled = offerMapper.markHandled(offer.getId(), OfferStatus.ACCEPTED.name(), LocalDateTime.now());
        if (handled == 0) {
            throw BizException.stateConflict("报价状态已变更，请刷新后重试");
        }
        int locked = productMapper.casStatus(product.getId(),
                ProductStatus.ON_SALE.name(), ProductStatus.LOCKED.name());
        if (locked == 0) {
            throw BizException.stateConflict("商品刚被其他人买走，报价无法成交");
        }
        Long orderId = createOrder(offer, product);
        notificationService.send(offer.getSellerId(), "BARGAIN", "报价已自动成交",
                "「" + product.getTitle() + "」收到达到底价的报价 " + offer.getOfferPrice() + " 元，已生成订单", orderId);
        notificationService.send(offer.getBuyerId(), "BARGAIN", "已自动成交",
                "你的出价达到卖家底价，「" + product.getTitle() + "」订单已生成，请尽快支付", orderId);
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long accept(Long offerId) {
        BargainOffer offer = requireOffer(offerId);
        Long sellerId = SecurityUtils.currentUserId();
        if (!offer.getSellerId().equals(sellerId)) {
            throw BizException.ownershipDenied();
        }
        if (!OfferStatus.PENDING.name().equals(offer.getStatus())) {
            throw BizException.stateConflict("该报价已被处理或已过期");
        }
        Product product = productMapper.selectById(offer.getProductId());
        if (product == null || !ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw BizException.stateConflict("商品已下架或已被他人购买，无法接受该报价");
        }

        // 1) 报价单 CAS
        int handled = offerMapper.markHandled(offerId, OfferStatus.ACCEPTED.name(), LocalDateTime.now());
        if (handled == 0) {
            throw BizException.stateConflict("该报价已被处理，请刷新");
        }
        // 2) 商品 CAS：失败会抛异常，事务回滚上面那一步
        int locked = productMapper.casStatus(product.getId(),
                ProductStatus.ON_SALE.name(), ProductStatus.LOCKED.name());
        if (locked == 0) {
            throw BizException.stateConflict("商品刚被其他人买走，报价无法成交");
        }

        // 3) 生成议价订单
        Long orderId = createOrder(offer, product);

        notificationService.send(offer.getBuyerId(), "BARGAIN", "卖家接受了你的还价",
                "「" + product.getTitle() + "」以 " + offer.getOfferPrice() + " 元成交，请尽快支付", orderId);
        log.info("议价成交 offerId={} orderId={} amount={}", offerId, orderId, offer.getOfferPrice());
        return orderId;
    }

    /** 生成议价订单（人工接受与底价自动成交共用，保证两条路径完全一致） */
    private Long createOrder(BargainOffer offer, Product product) {
        Order order = new Order();
        order.setOrderNo("ORD" + LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        order.setProductId(product.getId());
        order.setBuyerId(offer.getBuyerId());
        order.setSellerId(offer.getSellerId());
        order.setAmount(offer.getOfferPrice());
        order.setOrigin("BARGAIN");
        order.setOfferId(offer.getId());
        order.setStatus(OrderStatus.UNPAID.name());
        // 显式写创建时间：超时取消任务按 create_time 判断，依赖数据库默认值会因时区偏差被立即取消
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long offerId) {
        BargainOffer offer = requireOffer(offerId);
        Long sellerId = SecurityUtils.currentUserId();
        if (!offer.getSellerId().equals(sellerId)) {
            throw BizException.ownershipDenied();
        }
        int rows = offerMapper.markHandled(offerId, OfferStatus.REJECTED.name(), LocalDateTime.now());
        if (rows == 0) {
            throw BizException.stateConflict("该报价已被处理或已过期");
        }
        notificationService.send(offer.getBuyerId(), "BARGAIN", "卖家拒绝了你的还价",
                "你可以调整价格后再次出价", offer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long offerId) {
        BargainOffer offer = requireOffer(offerId);
        Long buyerId = SecurityUtils.currentUserId();
        if (!offer.getBuyerId().equals(buyerId)) {
            throw BizException.ownershipDenied();
        }
        int rows = offerMapper.markHandled(offerId, OfferStatus.CANCELLED.name(), LocalDateTime.now());
        if (rows == 0) {
            throw BizException.stateConflict("该报价已被处理或已过期");
        }
    }

    @Override
    public PageResult<OfferResponse> pageSent(int page, int size) {
        Long buyerId = SecurityUtils.currentUserId();
        return pageBy(Wrappers.<BargainOffer>lambdaQuery().eq(BargainOffer::getBuyerId, buyerId), page, size);
    }

    @Override
    public PageResult<OfferResponse> pageReceived(int page, int size) {
        Long sellerId = SecurityUtils.currentUserId();
        return pageBy(Wrappers.<BargainOffer>lambdaQuery().eq(BargainOffer::getSellerId, sellerId), page, size);
    }

    @Override
    public List<Long> findExpiredIds(int limit) {
        return offerMapper.selectExpired(LocalDateTime.now(), limit).stream()
                .map(BargainOffer::getId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireOne(Long offerId) {
        BargainOffer offer = offerMapper.selectById(offerId);
        if (offer == null || !OfferStatus.PENDING.name().equals(offer.getStatus())) {
            return;
        }
        offerMapper.markHandled(offerId, OfferStatus.EXPIRED.name(), LocalDateTime.now());
        notificationService.send(offer.getBuyerId(), "BARGAIN", "还价已过期",
                "卖家超过 24 小时未处理你的报价，可重新出价", offerId);
        notificationService.send(offer.getSellerId(), "BARGAIN", "还价已过期",
                "你对一条报价未及时处理，该报价已自动过期", offerId);
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private PageResult<OfferResponse> pageBy(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BargainOffer> wrapper,
            int page, int size) {
        wrapper.orderByDesc(BargainOffer::getCreateTime);
        Page<BargainOffer> result = offerMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), wrapper);
        List<BargainOffer> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }

        Map<Long, Product> products = batchProducts(records.stream().map(BargainOffer::getProductId).toList());
        Map<Long, SysUser> users = batchUsers(records.stream()
                .flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId()))
                .toList());
        LoginUser current = SecurityUtils.currentUser();

        List<OfferResponse> list = records.stream().map(o -> {
            Product product = products.get(o.getProductId());
            SysUser buyer = users.get(o.getBuyerId());
            SysUser seller = users.get(o.getSellerId());
            boolean pending = OfferStatus.PENDING.name().equals(o.getStatus());
            return OfferResponse.builder()
                    .id(o.getId())
                    .productId(o.getProductId())
                    .productTitle(product == null ? null : product.getTitle())
                    .productCover(product == null ? null : product.getCoverUrl())
                    .productPrice(product == null ? null : product.getPrice())
                    .offerPrice(o.getOfferPrice())
                    .message(o.getMessage())
                    .status(o.getStatus())
                    .statusText(statusText(o.getStatus()))
                    .buyerId(o.getBuyerId())
                    .buyerNickname(buyer == null ? null : buyer.getNickname())
                    .sellerId(o.getSellerId())
                    .sellerNickname(seller == null ? null : seller.getNickname())
                    .expireTime(o.getExpireTime())
                    .handledTime(o.getHandledTime())
                    .createTime(o.getCreateTime())
                    .canHandle(pending && current.getUserId().equals(o.getSellerId()))
                    .canCancel(pending && current.getUserId().equals(o.getBuyerId()))
                    .build();
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private BargainOffer requireOffer(Long offerId) {
        BargainOffer offer = offerId == null ? null : offerMapper.selectById(offerId);
        if (offer == null) {
            throw new BizException(ResultCode.NOT_FOUND, "报价单不存在");
        }
        return offer;
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

    private String statusText(String status) {
        try {
            return OfferStatus.valueOf(status).getDescription();
        } catch (Exception e) {
            return status;
        }
    }
}
