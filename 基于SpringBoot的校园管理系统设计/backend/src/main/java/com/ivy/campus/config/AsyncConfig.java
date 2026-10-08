package com.ivy.campus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置，用于公告推送、站内通知等后台任务
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private final ThreadPoolTaskExecutor pushExecutor = buildExecutor();

    /** 推送专用线程池：核心 4、最大 16、队列 200 */
    private ThreadPoolTaskExecutor buildExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("campus-push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        log.info("异步推送线程池装配完成 core=4 max=16 queue=200");
        return executor;
    }

    @Bean("pushExecutor")
    public ThreadPoolTaskExecutor pushExecutor() {
        return pushExecutor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return pushExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
