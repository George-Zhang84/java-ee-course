package com.zhj.exp4.filter;

import com.zhj.exp4.common.TraceIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器：为每个请求生成 traceId，写入 MDC 与响应头。
 *
 * <p>作用：前端报错时把响应里的 {@code traceId} 报给后端，即可在日志中精确定位该次请求，
 * 这也是统一响应体里带 traceId 的原因。
 *
 * <p>{@code OncePerRequestFilter} 保证一次请求只执行一次（转发/异步不会重复生成）；
 * 必须在 finally 中清理 MDC 与 ThreadLocal，否则线程复用时 traceId 会串。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先复用网关/前端传入的 traceId，便于全链路串联
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        TraceIdHolder.set(traceId);
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            TraceIdHolder.clear();
        }
    }
}
