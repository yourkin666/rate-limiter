package com.example.ratelimiter.algorithm;

/**
 * 令牌桶限流器
 * 
 * @author yourkin666
 * @version 1.1.0 - Simplified
 */
public class TokenBucket {

    /** 桶容量 */
    private final double capacity;

    /** 令牌补充速率（每秒） */
    private final double refillRate;

    /** 当前令牌数 */
    private double tokens;

    /** 上次更新时间 */
    private long lastTime;

    /**
     * 构造函数
     * 
     * @param qps 每秒请求次数
     */
    public TokenBucket(double qps) {
        this.capacity = qps;
        this.refillRate = qps;
        this.tokens = qps;
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * 尝试获取令牌
     * 
     * @return true-获取成功，false-获取失败
     */
    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /**
     * 补充令牌
     */
    private void refill() {
        long now = System.currentTimeMillis();
        double tokensToAdd = (now - lastTime) / 1000.0 * refillRate;
        tokens = Math.min(capacity, tokens + tokensToAdd);
        lastTime = now;
    }
}