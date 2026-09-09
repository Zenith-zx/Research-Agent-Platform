package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.config.EarningsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.LockSupport;

@Component
public class SecRateLimiter {
    @Resource
    private EarningsProperties properties;

    private long intervalNanos;

    private long nextRequestNanos;

    /**
     * 初始化 SEC 请求最小间隔。
     */
    @PostConstruct
    void initialize() {
        intervalNanos = properties.getSec().getMinRequestInterval().toNanos();
    }

    /**
     * 等待到下一个允许发送 SEC 请求的时间点。
     */
    public synchronized void acquire() {
        long now = System.nanoTime();
        if (now < nextRequestNanos) {
            LockSupport.parkNanos(nextRequestNanos - now);
        }
        nextRequestNanos = System.nanoTime() + intervalNanos;
    }
}
