package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体（review）。一个订单中，买家和卖家各可评价一次，
 * 唯一索引 {@code uk_order_reviewer} 保证同一人在同一订单只能评一次。
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    /** 评价人 */
    private Long reviewerId;

    /** 被评价人 */
    private Long revieweeId;

    /** BUYER：买家评卖家（展示在商品详情）；SELLER：卖家评买家 */
    private String reviewerRole;

    /** 1~5 星 */
    private Integer score;

    private String content;

    private LocalDateTime createTime;
}
