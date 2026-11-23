package com.zhj.exp4.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>约定：Service 层遇到"业务规则不允许"的情况直接抛出该异常，
 * 由 {@link GlobalExceptionHandler} 统一转换成 HTTP 200 + 业务错误码，
 * 避免把技术异常（如空指针、SQL 异常）暴露给前端。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /** 业务校验失败时使用，等价于 new BizException(ResultCode.BIZ_ERROR, message) */
    public static BizException of(String message) {
        return new BizException(ResultCode.BIZ_ERROR, message);
    }

    /** 状态机冲突：如订单已支付仍尝试支付 */
    public static BizException stateConflict(String message) {
        return new BizException(ResultCode.STATE_CONFLICT, message);
    }

    /** 水平越权：资源不属于当前用户 */
    public static BizException ownershipDenied() {
        return new BizException(ResultCode.OWNERSHIP_DENIED);
    }
}
