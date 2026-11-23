package com.zhj.exp4.common;

import lombok.Data;

/**
 * 统一响应体。
 *
 * <p>前后端约定：HTTP 状态码只表达传输层结果，业务结果一律看 {@code code}。
 * <ul>
 *   <li>{@code code == 0} 表示成功；</li>
 *   <li>其余为错误码，含义见 {@link ResultCode}；</li>
 *   <li>{@code traceId} 便于用户报错时定位后端日志。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /** 业务状态码，0 表示成功 */
    private int code;
    /** 提示信息，前端可直接 toast */
    private String message;
    /** 业务数据 */
    private T data;
    /** 链路追踪 id，与日志中的 %X{traceId} 对应 */
    private String traceId;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = TraceIdHolder.current();
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
