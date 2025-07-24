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

    /**
     * 限流的唯一标识。
     * 比如可以根据用户ID限流，也可以根据IP地址限流。
     * 默认是 "all"，表示所有请求都算在一起，进行全局限流。
     */
    String key() default "all";

    /**
     * 每秒允许的请求数 (Queries Per Second)。
     * 这是一个必须填写的项，因为它定义了限流的阈值。
     */
    double qps();

    /**
     * 进入黑名单的触发次数。
     * 比如设置为10，表示如果一个key连续被限流10次，就将其拉黑一段时间。
     * 默认是 0，表示这个功能默认不开启。
     */
    int blacklistCount() default 0;

    /**
     * 当请求被限流后，应该去调用哪个“备用方案”方法。
     * 这是一个必须填写的项，用于服务降级。
     */
    String fallback();
}