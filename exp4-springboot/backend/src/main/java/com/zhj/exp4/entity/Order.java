package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（order）。
 *
 * <p>表名 {@code order} 是 MySQL 保留字，因此 {@code @TableName} 必须带反引号，
 * 否则 MyBatis-Plus 生成的 {@code SELECT ... FROM order} 会报语法错误。
 *
 * <p>状态机（由 {@code OrderStatus} 枚举约束）：
 * <pre>
 *   UNPAID --支付--> PAID --卖家发货--> SHIPPED --买家收货--> COMPLETED
 *   UNPAID --取消/超时--> CANCELLED
 * </pre>
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务单号，形如 ORD202512211530120001 */
    private String orderNo;

    private Long productId;

    private Long buyerId;

    private Long sellerId;

    /** 成交金额：直接购买为商品价，议价成交为报价单金额（快照，不随商品改价变化） */
    private BigDecimal amount;

    /** DIRECT 直接购买 / BARGAIN 议价成交 */
    private String origin;

    /** 议价成交时对应的报价单 id，唯一索引防止一张报价单成交两次 */
    private Long offerId;

    private String status;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime finishTime;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
