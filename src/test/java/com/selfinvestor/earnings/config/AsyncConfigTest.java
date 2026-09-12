package com.selfinvestor.earnings.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    /**
     * 验证 Filing 线程池满载时使用调用线程施加反压而不是拒绝任务。
     */
    @Test
    void filingExecutorUsesCallerRunsBackpressure() {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.filingExecutor();
        try {
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
