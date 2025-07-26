package com.example.ratelimiter.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 
 * @author yourkin666
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface RateLimit {

    String key() default "all";
    
    double qps();
    
    int blacklistCount() default 0;
    
    String fallback();
}