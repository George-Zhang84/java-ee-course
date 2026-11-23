package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 议价报价单实体（bargain_offer）—— 本系统特色功能 S1 的核心数据结构。
 *
 * <p>把"闲鱼聊天里砍价"变成结构化记录，状态机：
 * <pre>
 *   PENDING --卖家接受--> ACCEPTED（同时锁定商品并生成订单）
 *   PENDING --卖家拒绝--> REJECTED
 *   PENDING --买家撤回--> CANCELLED
 *   PENDING --24h 未处理--> EXPIRED（定时任务）
 *   ACCEPTED --订单超时取消--> EXPIRED（回滚商品）
 * </pre>
 */
@Data
@TableName("bargain_offer")
public class BargainOffer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long buyerId;

    private Long sellerId;

    /** 买家出价金额 */
    private BigDecimal offerPrice;

    /** 买家留言 */
    private String message;

    private String status;

    /** 过期时间 = 创建时间 + 配置的有效期（默认 24h） */
    private LocalDateTime expireTime;

    /** 卖家处理时间 */
    private LocalDateTime handledTime;

    private LocalDateTime createTime;
}
