package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.request.OrderQuery;
import com.zhj.exp4.dto.response.OrderResponse;

import java.util.List;

/**
 * 订单服务。
 *
 * <p>订单是本系统状态最复杂的聚合：状态流转、资金流转、商品状态三者必须保持一致，
 * 因此所有写操作都遵循同一套模板：
 * <pre>
 *   1) 校验参与方（是不是买家/卖家）
 *   2) 用 CAS 更新订单状态（防重复支付/并发取消）
 *   3) 联动商品状态（LOCKED -> SOLD / ON_SALE）
 *   4) 联动资金（扣款、入账、写流水）
 *   5) 发通知 / 发事件
 * </pre>
 * 2~4 在同一事务内，任何一步失败整体回滚。
 */
public interface OrderService {

    /**
     * 直接购买：锁定商品并生成待支付订单。
     *
     * @param productId 商品 id
     * @return 订单详情
     */
    OrderResponse create(Long productId);

    /**
     * 支付：扣买家余额、给卖家入账、订单转 PAID。
     *
     * @param orderId 订单 id
     */
    void pay(Long orderId);

    /**
     * 卖家发货。
     *
     * @param orderId 订单 id
     */
    void ship(Long orderId);

    /**
     * 买家确认收货。
     *
     * @param orderId 订单 id
     */
    void confirm(Long orderId);

    /**
     * 买家取消未支付订单（或卖家拒单）。
     *
     * @param orderId 订单 id
     */
    void cancel(Long orderId);

    /** 我的订单分页（买家视角 / 卖家视角） */
    PageResult<OrderResponse> page(OrderQuery query);

    /** 订单详情（仅买卖双方与管理员可见） */
    OrderResponse detail(Long orderId);

    /**
     * 查询超时未支付的订单 id（定时任务用，避免在事务里做全表扫描）。
     *
     * @param limit 单次上限
     * @return 订单 id 列表
     */
    List<Long> findTimeoutOrderIds(int limit);

    /**
     * 系统取消超时订单（定时任务逐条调用，保证每条一个独立事务）。
     *
     * @param orderId 订单 id
     */
    void cancelBySystem(Long orderId);
}
