package com.zhj.exp4.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应结构。
 *
 * <p>与 MyBatis-Plus 的 {@link IPage} 解耦：Controller 层统一转成该结构，
 * 前端只需要认 {@code records/total/page/size} 四个字段。
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private long page;
    /** 每页条数 */
    private long size;

    /** 直接由 MyBatis-Plus 分页对象构建（不做类型转换） */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 由分页对象构建并把 PO 映射为 VO */
    public static <P, T> PageResult<T> of(IPage<P> page, Function<P, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 空分页 */
    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }
}
