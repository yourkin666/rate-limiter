package com.example.ratelimiter.config;

import com.example.ratelimiter.aop.RateLimitAOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流组件自动配置类
 * 
 * @author yourkin666
 * @version 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterAutoConfiguration.class);

    /**
     * 注册限流AOP切面Bean
     * 
     * @return RateLimitAOP实例
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAOP.class)
    public RateLimitAOP rateLimitAOP() {
        log.info("限流组件初始化完成，Rate Limiter Spring Boot Starter v1.0.0");
        return new RateLimitAOP();
    }
}