package com.zhj.exp4.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Spring MVC 配置：静态资源映射 + 跨域。
 *
 * <p>两个职责：
 * <ol>
 *   <li>把上传目录（默认 {@code ./uploads}）映射为 {@code /uploads/**} 访问路径，
 *       前端拿到的是相对 URL，部署到 Nginx 时只需改这一处前缀；</li>
 *   <li>开发期允许 Vite 开发服务器（5173）跨域直连后端。生产环境前后端同源，
 *       不依赖此配置，但保留也不会产生安全问题（未开放 allowCredentials）。</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一转成绝对路径 + 结尾斜杠，Windows/Linux 通用
        String uploadDir = new File(properties.getUpload().getDir()).getAbsolutePath();
        String location = "file:" + uploadDir.replace("\\", "/") + "/";
        registry.addResourceHandler(properties.getUpload().getUrlPrefix() + "/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .maxAge(3600);
    }
}
