package com.ivy.campus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI campusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("加利顿大学 校园公告管理系统 API")
                        .description("公告发布、审批流转、回执统计、通知推送一体化接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("加利顿大学信息中心").email("campus@jialidun.edu.cn"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi announcementApi() {
        return GroupedOpenApi.builder()
                .group("公告管理")
                .displayName("公告管理")
                .pathsToMatch("/api/announcement/**", "/api/category/**", "/api/comment/**",
                        "/api/template/**", "/api/file/**", "/api/receipt/**", "/api/stats/**")
                .build();
    }

    @Bean
    public GroupedOpenApi approvalApi() {
        return GroupedOpenApi.builder()
                .group("审批")
                .displayName("审批")
                .pathsToMatch("/api/approval/**", "/api/notice/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统管理")
                .displayName("系统管理")
                .pathsToMatch("/api/auth/**", "/api/user/**", "/api/dept/**", "/api/log/**")
                .build();
    }
}
