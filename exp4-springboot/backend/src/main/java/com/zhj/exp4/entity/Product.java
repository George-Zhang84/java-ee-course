package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（product）。
 *
 * <p>status 是核心状态机字段，流转规则由 {@code ProductStatus} 与 Service 层共同保证：
 * <pre>
 *   PENDING --审核通过--> ON_SALE --下单/议价成交--> LOCKED --支付完成--> SOLD
 *   PENDING --驳回--> REJECTED
 *   ON_SALE --卖家下架--> OFF_SHELF
 *   LOCKED  --订单取消--> ON_SALE（回滚）
 * </pre>
 *
 * <p>{@code minPrice} 是卖家底价，只在"议价自动成交"判断时使用，
 * 查询详情时会被置空，防止买家探测底价。
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sellerId;

    private Long categoryId;

    private String title;

    private String description;

    private BigDecimal price;

    /** 卖家心理底价，不对外暴露 */
    private BigDecimal minPrice;

    /** 1 允许议价 / 0 一口价 */
    private Integer allowBargain;

    /** 成色：全新 / 99新 / 95新 / 9成新 / 8成新 */
    private String conditionLevel;

    /** 校内交易地点 */
    private String location;

    private String coverUrl;

    private String status;

    /** 审核意见（驳回原因） */
    private String auditRemark;

    private Integer views;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
