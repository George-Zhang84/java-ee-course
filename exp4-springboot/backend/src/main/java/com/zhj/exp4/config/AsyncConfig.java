package com.zhj.exp4.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * <p>审计日志、站内通知等"非关键路径"操作放到独立线程池，
 * 避免拖慢主请求；同时用有界队列 + CallerRunsPolicy 做背压，
 * 队列打满时退化为同步执行，保证日志不丢。
 *
 * <p>注意：异步方法必须在另一个 Bean 中调用才会生效（Spring AOP 代理限制）。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AUDIT_EXECUTOR = "auditExecutor";

    @Bean(AUDIT_EXECUTOR)
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
