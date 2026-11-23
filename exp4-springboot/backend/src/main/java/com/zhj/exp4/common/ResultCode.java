package com.zhj.exp4.common;

import lombok.Getter;

/**
 * 业务错误码。
 *
 * <p>编码规则：1xxx 请求/认证类，2xxx 业务规则类，5xxx 系统类。
 * 新增错误码时只允许追加，禁止修改已有编码，避免前端判断失效。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "ok"),

    /** 参数校验失败（Bean Validation 或手工校验） */
    PARAM_ERROR(1001, "参数不合法"),
    /** 未登录、令牌缺失/过期/被吊销 */
    UNAUTHORIZED(1002, "登录状态已失效，请重新登录"),
    /** 已登录但权限不足（纵向越权） */
    FORBIDDEN(1003, "没有权限执行该操作"),
    /** 资源不存在 */
    NOT_FOUND(1004, "资源不存在"),
    /** 请求过于频繁（登录失败次数过多等） */
    TOO_MANY_REQUESTS(1005, "操作过于频繁，请稍后再试"),

    /** 通用业务规则拒绝，具体原因写在 message 中 */
    BIZ_ERROR(2001, "业务操作失败"),
    /** 状态机流转非法，如重复支付、已售出商品再次下单 */
    STATE_CONFLICT(2002, "当前状态不允许该操作"),
    /** 资源归属校验失败（水平越权） */
    OWNERSHIP_DENIED(2003, "无权操作他人资源"),
    /** 余额不足 */
    BALANCE_NOT_ENOUGH(2004, "钱包余额不足"),
    /** 重复提交（幂等键冲突） */
    REPEAT_SUBMIT(2005, "请勿重复提交"),

    /** 未捕获异常 */
    SYSTEM_ERROR(5000, "系统繁忙，请稍后再试"),
    /** 上传文件不合法 */
    FILE_ERROR(5001, "文件不符合要求");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
