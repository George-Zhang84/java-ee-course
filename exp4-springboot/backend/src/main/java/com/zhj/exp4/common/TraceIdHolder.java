package com.zhj.exp4.common;

/**
 * 线程级 traceId 持有器。
 *
 * <p>由 {@code TraceIdFilter} 在请求入口生成并放入 MDC，响应体与日志共用同一个 id；
 * 使用 ThreadLocal 保存，请求结束必须 {@link #clear()}，否则线程池复用会造成串号。
 */
public final class TraceIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceIdHolder() {
    }

    public static void set(String traceId) {
        HOLDER.set(traceId);
    }

    public static String current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
