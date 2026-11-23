package com.zhj.exp4.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务自定义配置，绑定 application.yml 中的 {@code exp4.*}。
 *
 * <p>相比到处 {@code @Value("${...}")}，集中式配置对象的优点：
 * <ul>
 *   <li>类型安全、IDE 可跳转、有默认值；</li>
 *   <li>支持环境变量覆盖（如 {@code JWT_SECRET}、{@code ORDER_TIMEOUT_MINUTES}）；</li>
 *   <li>单测中可以整体替换，无需启动容器。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "exp4")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Order order = new Order();
    private Offer offer = new Offer();

    @Data
    public static class Jwt {
        /** 签名密钥，生产必须通过 JWT_SECRET 环境变量覆盖且长度 >= 32 字节 */
        private String secret;
        /** 令牌有效期（秒） */
        private long expireSeconds = 7200L;
    }

    @Data
    public static class Upload {
        /** 本地存储根目录 */
        private String dir = "./uploads";
        /** 对外访问前缀，与 WebMvcConfig 的资源映射保持一致 */
        private String urlPrefix = "/uploads";
        /** 单文件大小上限（MB） */
        private int maxSizeMb = 5;
    }

    @Data
    public static class Order {
        /** 未支付超时分钟数，定时任务据此取消订单 */
        private long timeoutMinutes = 15L;
        /** 超时订单扫描间隔（毫秒） */
        private long timeoutScanMs = 60000L;
    }

    @Data
    public static class Offer {
        /** 报价单有效期（小时） */
        private long expireHours = 24L;
        /** 同一买家对同一商品的最大出价次数 */
        private int maxAttempts = 3;
        /** 过期报价单扫描间隔（毫秒） */
        private long expireScanMs = 60000L;
    }
}
