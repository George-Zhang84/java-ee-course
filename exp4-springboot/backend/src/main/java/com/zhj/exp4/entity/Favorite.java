package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹实体（favorite）。
 *
 * <p>唯一索引 {@code uk_user_product} 让"重复收藏"天然幂等：
 * 并发重复提交时数据库会拒绝第二条，Service 层捕获后按"已收藏"处理即可。
 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    private LocalDateTime createTime;
}
