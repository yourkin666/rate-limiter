package com.example.ratelimiter.algorithm;

/**
 * 令牌桶限流器
 * 
 * @author yourkin666
 * @version 1.1.0 - Simplified
 */
public class TokenBucket {

    private final double qps;
    private double tokens;
    private long lastTime;

    public TokenBucket(double qps) {
        this.qps = qps;
        this.tokens = qps;
        this.lastTime = System.currentTimeMillis();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double tokensToAdd = (now - lastTime) / 1000.0 * qps;
        tokens = Math.min(qps, tokens + tokensToAdd);
        lastTime = now;
    }
}