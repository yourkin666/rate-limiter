package com.example.ratelimiter.aop;

import com.example.ratelimiter.algorithm.TokenBucket;
import com.example.ratelimiter.annotation.RateLimit;
import com.example.ratelimiter.cache.LocalCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.Order;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流AOP切面
 * 
 * @author yourkin666
 * @version 1.0.0
 */
@Aspect
@Order(1)
public class RateLimitAOP {

    private static final Log log = LogFactory.getLog(RateLimitAOP.class);

    private static final long BUCKET_TTL = 60 * 1000L; // 1分钟
    private static final long BLACKLIST_TTL = 24 * 60 * 60 * 1000L; // 24小时

    private final LocalCache<String, TokenBucket> bucketCache = new LocalCache<>(BUCKET_TTL);
    private final LocalCache<String, Long> blacklist = new LocalCache<>(BLACKLIST_TTL);

    private final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.example.ratelimiter.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }

    /**
     * 环绕通知：执行限流逻辑
     * 
     * @param jp 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("rateLimitPointcut()")
    public Object doRateLimit(ProceedingJoinPoint jp) throws Throwable {
        Method method = getMethod(jp);
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 提取限流标识
        String key = getAttrValue(rateLimit.key(), jp.getArgs());
        log.info("限流标识：{}", key);

        // 黑名单检查
        if (!"all".equals(key) && rateLimit.blacklistCount() != 0) {
            Long blackCount = blacklist.get(key);
            if (blackCount != null && blackCount > rateLimit.blacklistCount()) {
                log.info("限流-黑名单拦截(24h)：{}", key);
                return invokeCallback(jp, rateLimit.fallback());
            }
        }

        // 获取或创建令牌桶
        TokenBucket bucket = bucketCache.get(key);
        if (bucket == null) {
            bucket = new TokenBucket(rateLimit.qps());
            bucketCache.put(key, bucket);
        }

        // 尝试获取令牌
        if (!bucket.tryAcquire()) {
            log.info("限流-超频次拦截：{}", key);

            // 增加黑名单计数
            if (!"all".equals(key) && rateLimit.blacklistCount() != 0) {
                Long count = blacklist.get(key);
                count = count == null ? 1 : count + 1;
                blacklist.put(key, count);
            }

            return invokeCallback(jp, rateLimit.fallback());
        }

        // 执行原方法
        return jp.proceed();
    }

    /**
     * 获取方法对象 - 带缓存优化
     * 
     * @param jp 连接点
     * @return 方法对象
     * @throws NoSuchMethodException 方法不存在异常
     */
    private Method getMethod(ProceedingJoinPoint jp) throws NoSuchMethodException {
        String methodKey = jp.getSignature().toLongString();
        
        return methodCache.computeIfAbsent(methodKey, key -> {
            try {
                String methodName = jp.getSignature().getName();
                Class<?>[] parameterTypes = new Class[jp.getArgs().length];
                Object[] args = jp.getArgs();
                for (int i = 0; i < args.length; i++) {
                    parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
                }
                return jp.getTarget().getClass().getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 提取属性值 - 简化版
     * 
     * @param attr 属性名
     * @param args 方法参数
     * @return 属性值
     */
    private String getAttrValue(String attr, Object[] args) {
        if ("all".equals(attr) || args == null || args.length == 0) {
            return attr;
        }

        // 优先查找字符串参数
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }

        // 反射获取对象属性
        for (Object arg : args) {
            if (arg != null) {
                try {
                    java.lang.reflect.Field field = arg.getClass().getDeclaredField(attr);
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (Exception ignored) {
                    // 忽略异常继续
                }
            }
        }

        return attr;
    }

    /**
     * 执行回调方法 - 优化版
     * 
     * @param jp             连接点
     * @param fallbackMethod 回调方法名
     * @return 回调方法执行结果
     * @throws Exception 异常
     */
    private Object invokeCallback(ProceedingJoinPoint jp, String fallbackMethod) throws Exception {
        if (fallbackMethod == null || fallbackMethod.trim().isEmpty()) {
            throw new RuntimeException("限流触发，但未配置fallback方法");
        }

        String fallbackKey = jp.getTarget().getClass().getName() + "#" + fallbackMethod;
        Method method = methodCache.computeIfAbsent(fallbackKey, key -> {
            try {
                Class<?>[] parameterTypes = getParameterTypes(jp);
                return jp.getTarget().getClass().getMethod(fallbackMethod, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("找不到fallback方法：" + fallbackMethod, e);
            }
        });

        try {
            return method.invoke(jp.getTarget(), jp.getArgs());
        } catch (Exception e) {
            log.error("执行fallback方法失败：{}", fallbackMethod, e);
            throw e;
        }
    }

    /**
     * 获取方法参数类型数组
     * 
     * @param jp 连接点
     * @return 参数类型数组
     */
    private Class<?>[] getParameterTypes(ProceedingJoinPoint jp) {
        Object[] args = jp.getArgs();
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return parameterTypes;
    }
}