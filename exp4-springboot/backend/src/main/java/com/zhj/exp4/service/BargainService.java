package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.request.OfferCreateRequest;
import com.zhj.exp4.dto.response.OfferCreateResponse;
import com.zhj.exp4.dto.response.OfferResponse;

import java.util.List;

/**
 * 议价服务（特色功能 S1）。
 *
 * <p>业务规则（全部在实现类中校验，前端只是提示层）：
 * <ul>
 *   <li>只有 ON_SALE 且 allowBargain=1 的商品可以还价；</li>
 *   <li>不能对自己的商品出价；出价必须低于售价；</li>
 *   <li>同一买家对同一商品同时只能有 1 条待处理报价，累计最多 3 次；</li>
 *   <li>报价单 24 小时未处理自动过期；</li>
 *   <li>卖家接受时原子锁定商品并生成订单，订单超时取消后报价单作废。</li>
 * </ul>
 */
public interface BargainService {

    /**
     * 买家发起报价。
     *
     * <p>如果卖家设置了底价且本次出价 ≥ 底价，会立即自动成交并生成订单，
     * 返回值中的 {@code autoAccepted=true}，前端据此直接引导支付。
     *
     * @param request 报价参数
     * @return 报价结果（含是否自动成交与订单 id）
     */
    OfferCreateResponse createOffer(OfferCreateRequest request);

    /**
     * 卖家接受报价：锁定商品并生成订单。
     *
     * @param offerId 报价单 id
     * @return 生成的订单 id
     */
    Long accept(Long offerId);

    /** 卖家拒绝报价 */
    void reject(Long offerId);

    /** 买家撤回报价 */
    void cancel(Long offerId);

    /** 我发出的报价（买家视角） */
    PageResult<OfferResponse> pageSent(int page, int size);

    /** 我收到的报价（卖家视角） */
    PageResult<OfferResponse> pageReceived(int page, int size);

    /**
     * 已过期的待处理报价单 id（定时任务用）。
     *
     * @param limit 单次上限
     */
    List<Long> findExpiredIds(int limit);

    /**
     * 把单条报价置为过期（定时任务逐条调用，各自独立事务）。
     *
     * @param offerId 报价单 id
     */
    void expireOne(Long offerId);
}
