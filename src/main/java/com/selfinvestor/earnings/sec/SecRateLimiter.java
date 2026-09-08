package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.config.EarningsProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

@Component
public class SecRateLimiter {
    private final long intervalNanos;
    private long nextRequestNanos;

    public SecRateLimiter(EarningsProperties properties) {
        this.intervalNanos = properties.getSec().getMinRequestInterval().toNanos();
    }

    public synchronized void acquire() {
        long now = System.nanoTime();
        if (now < nextRequestNanos) LockSupport.parkNanos(nextRequestNanos - now);
        nextRequestNanos = System.nanoTime() + intervalNanos;
    }
}
