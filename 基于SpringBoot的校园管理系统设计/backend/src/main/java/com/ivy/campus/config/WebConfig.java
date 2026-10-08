package com.ivy.campus.config;

import com.ivy.campus.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 配置：拦截器、跨域、上传目录静态映射
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${campus.upload.path:D:/campus-upload/}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/captcha",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/favicon.ico",
                        "/upload/**",
                        "/error"
                );
        log.info("鉴权拦截器注册完成，拦截 /api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
        log.info("跨域配置注册完成");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = normalizeLocation(uploadPath);
        registry.addResourceHandler("/upload/**").addResourceLocations(location);
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        log.info("静态资源映射完成 /upload/** -> {}", location);
    }

    /** 统一目录格式，确保以 file: 与斜杠结尾 */
    private String normalizeLocation(String path) {
        String directory = (path == null || path.isBlank()) ? "D:/campus-upload/" : path.trim();
        if (!directory.endsWith("/") && !directory.endsWith(File.separator)) {
            directory = directory + "/";
        }
        return directory.startsWith("file:") ? directory : "file:" + directory;
    }
}
