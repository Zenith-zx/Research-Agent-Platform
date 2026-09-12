package com.selfinvestor.earnings.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {
    /**
     * 创建 Filing 下载和解析线程池。
     *
     * @return Filing 线程池
     */
    @Bean(name = "filingExecutor")
    public Executor filingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("filing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 创建 AI 分析线程池。
     *
     * @return AI 分析线程池
     */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analysis-");
        executor.initialize();
        return executor;
    }

    /**
     * 创建市场压力事件处理线程池。
     *
     * @return 市场压力线程池
     */
    @Bean(name = "marketStressExecutor")
    public Executor marketStressExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("market-stress-");
        executor.initialize();
        return executor;
    }
}
