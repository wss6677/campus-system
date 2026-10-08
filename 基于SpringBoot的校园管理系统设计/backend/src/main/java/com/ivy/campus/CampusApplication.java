package com.ivy.campus;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 加利顿大学 校园公告管理系统启动类
 */
@Slf4j
@EnableAsync
@EnableScheduling
@MapperScan("com.ivy.campus.mapper")
@SpringBootApplication
public class CampusApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CampusApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profile = String.join(",", env.getActiveProfiles());
        log.info("\n========================================================\n" +
                        "  加利顿大学 校园公告管理系统 启动成功\n" +
                        "  本地接口地址 : http://localhost:{}{}\n" +
                        "  接口文档地址 : http://localhost:{}{}/doc.html\n" +
                        "  运行环境     : {}\n" +
                        "  数据库       : {}\n" +
                        "========================================================",
                port, contextPath, port, contextPath,
                profile.isEmpty() ? "default" : profile,
                env.getProperty("spring.datasource.url", "未配置"));
    }
}
