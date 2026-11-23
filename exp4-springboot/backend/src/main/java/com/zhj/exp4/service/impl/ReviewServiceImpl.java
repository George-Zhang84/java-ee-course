package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.dto.request.ReviewCreateRequest;
import com.zhj.exp4.dto.response.ReviewResponse;
import com.zhj.exp4.entity.Order;
import com.zhj.exp4.entity.Review;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.OrderStatus;
import com.zhj.exp4.mapper.OrderMapper;
import com.zhj.exp4.mapper.ReviewMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.NotificationService;
import com.zhj.exp4.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评价服务实现。
 *
 * <p>信用分规则（简单且可解释）：4~5 星 +1，3 星不变，1~2 星 -2，范围限制在 0~200。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ReviewCreateRequest request) {
        Long currentUserId = SecurityUtils.currentUserId();
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        boolean isBuyer = order.getBuyerId().equals(currentUserId);
        boolean isSeller = order.getSellerId().equals(currentUserId);
        if (!isBuyer && !isSeller) {
            throw BizException.ownershipDenied();
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw BizException.stateConflict("订单完成后才能评价");
        }
        Long exists = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getReviewerId, currentUserId));
        if (exists != null && exists > 0) {
            throw BizException.of("你已经评价过该订单了");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setProductId(order.getProductId());
        review.setReviewerId(currentUserId);
        review.setRevieweeId(isBuyer ? order.getSellerId() : order.getBuyerId());
        review.setReviewerRole(isBuyer ? "BUYER" : "SELLER");
        review.setScore(request.getScore());
        review.setContent(request.getContent());
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底
            throw BizException.of("你已经评价过该订单了");
        }

        // 信用分调整：好评加分、差评扣分
        int delta = request.getScore() >= 4 ? 1 : (request.getScore() <= 2 ? -2 : 0);
        if (delta != 0) {
            userMapper.adjustCreditScore(review.getRevieweeId(), delta);
        }
        notificationService.send(review.getRevieweeId(), "ORDER", "收到新的评价",
                "订单 " + order.getOrderNo() + " 收到 " + request.getScore() + " 星评价", order.getId());
        log.info("评价成功 orderId={} reviewer={} score={}", order.getId(), currentUserId, request.getScore());
    }

    @Override
    public PageResult<ReviewResponse> pageByProduct(Long productId, int page, int size) {
        Page<Review> result = reviewMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getProductId, productId)
                        .eq(Review::getReviewerRole, "BUYER")
                        .orderByDesc(Review::getCreateTime));
        return toPage(result);
    }

    @Override
    public PageResult<ReviewResponse> pageMine(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Page<Review> result = reviewMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getReviewerId, userId)
                        .orderByDesc(Review::getCreateTime));
        return toPage(result);
    }

    @Override
    public PageResult<ReviewResponse> pageReceived(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Page<Review> result = reviewMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreateTime));
        return toPage(result);
    }

    private PageResult<ReviewResponse> toPage(Page<Review> result) {
        List<Review> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        Map<Long, SysUser> users = batchUsers(records.stream().map(Review::getReviewerId).toList());
        List<ReviewResponse> list = records.stream().map(r -> {
            SysUser reviewer = users.get(r.getReviewerId());
            return ReviewResponse.builder()
                    .id(r.getId())
                    .orderId(r.getOrderId())
                    .productId(r.getProductId())
                    .score(r.getScore())
                    .content(r.getContent())
                    .reviewerRole(r.getReviewerRole())
                    .reviewerId(r.getReviewerId())
                    .reviewerNickname(reviewer == null ? null : reviewer.getNickname())
                    .revieweeId(r.getRevieweeId())
                    .createTime(r.getCreateTime())
                    .build();
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Map<Long, SysUser> batchUsers(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }
}
