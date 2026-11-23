package com.zhj.exp4.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>分页插件是必须显式注册的：没有它，{@code Page} 参数不会自动拼接 {@code LIMIT}，
 * 会退化成"全表查询再内存分页"，数据量大时是严重隐患。
 * 这里指定 {@link DbType#MYSQL} 以便生成 MySQL 方言的 count/limit 语句。
 */
@Configuration
public class MybatisPlusConfig {

    /** 单页最大条数，防止前端传入 size=100000 拖垮数据库 */
    private static final long MAX_PAGE_SIZE = 50L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        // 超出最大页时返回空列表而不是回到第一页，避免前端无限翻页
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
