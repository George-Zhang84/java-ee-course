package com.zhj.exp4;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手商品交易管理系统 —— 启动类。
 *
 * <p>三个注解的作用：
 * <ul>
 *   <li>{@code @SpringBootApplication}：组合注解，开启自动配置、组件扫描（com.zhj.exp4 包）与配置类注册；</li>
 *   <li>{@code @MapperScan}：扫描 MyBatis-Plus 的 Mapper 接口并生成代理实现，省去每个接口写 @Mapper；</li>
 *   <li>{@code @EnableScheduling}：开启定时任务，供订单超时取消、议价单过期使用；</li>
 *   <li>{@code @EnableAsync}：开启异步执行，供审计日志异步落库使用。</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.zhj.exp4.mapper")
@EnableScheduling
@EnableAsync
public class Exp4Application {

    public static void main(String[] args) {
        SpringApplication.run(Exp4Application.class, args);
    }
}
