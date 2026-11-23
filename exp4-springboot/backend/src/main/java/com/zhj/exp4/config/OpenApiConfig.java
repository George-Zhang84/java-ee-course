package com.zhj.exp4.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger UI）文档配置。
 *
 * <p>实验报告第 7 条"前后端交互接口设计"可直接引用 {@code /swagger-ui.html} 的截图；
 * 文档中注册了 Bearer 认证方式，测试受保护接口时先在右上角 Authorize 填入 token。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI exp4OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园二手商品交易管理系统 API")
                        .description("实验四 SpringBoot 后端接口文档（JWT 认证，统一响应 Result）")
                        .version("1.0.0")
                        .contact(new Contact().name("20240648 张豪杰")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
