package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情。
 *
 * <p>与列表项相比多了描述、多图、卖家信息与"是否已收藏"。
 * 同样不含底价；卖家自己查看时也不返回——底价只在服务端参与自动成交判断。
 */
@Data
@Builder
public class ProductDetailResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;

    /** 卖家底价：仅卖家本人与管理员可见，用于自动成交判断；普通买家为 null */
    private BigDecimal minPrice;

    private Integer allowBargain;
    private String conditionLevel;
    private String location;
    private String coverUrl;
    private List<String> images;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerNickname;
    private String sellerAvatar;
    private Integer sellerCreditScore;
    private String status;
    private String statusText;

    /** 审核意见（驳回原因）：仅卖家本人与管理员可见，普通买家为 null */
    private String auditRemark;

    private Integer views;
    private LocalDateTime createTime;

    /** 当前登录用户是否已收藏（未登录恒为 false） */
    private Boolean favorited;

    /** 当前用户是否是该商品卖家，前端据此决定是否显示"编辑/下架"按钮 */
    private Boolean owner;
}
