# Rate Limiter Spring Boot Starter

🚀 轻量级 Spring Boot 限流组件，零配置开箱即用，基于令牌桶算法 + 黑名单机制

## ✨ 核心优势

- **零配置**：引入依赖即可使用，无需任何配置
- **轻量级**：< 10KB，零外部依赖，性能 < 1ms
- **智能防护**：令牌桶 + 24h黑名单双重保护
- **灵活限流**：支持全局/用户/IP/自定义维度限流

## 🛠️ 快速使用

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 添加注解

```java
@RateLimit(
    key = "userId",           // 限流维度
    qps = 1.0,               // 每秒请求数
    fallback = "errorHandler", // 限流回调
    blacklistCount = 3       // 黑名单触发次数
)
@PostMapping("/api/draw")
public Response draw(@RequestBody Request request) {
    return success();
}

public Response errorHandler(@RequestBody Request request) {
    return error("请求过于频繁");
}
```

## 📋 注解参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `key` | 限流维度：`userId`/`all`/`clientIp` | `"all"` |
| `qps` | 每秒允许请求数 | 必填 |
| `fallback` | 限流回调方法名 | 必填 |
| `blacklistCount` | 黑名单触发次数 | `0` |

## 🎯 典型场景

### 用户抽奖限流
```java
@RateLimit(key = "userId", qps = 1.0, fallback = "drawError", blacklistCount = 1)
@PostMapping("/draw")
```

### API全局限流
```java
@RateLimit(key = "all", qps = 100.0, fallback = "systemBusy")
@GetMapping("/api/data")
```

### IP访问限制
```java
@RateLimit(key = "clientIp", qps = 10.0, fallback = "ipLimit", blacklistCount = 5)
@PostMapping("/comment")
```

## ⚙️ 配置选项

```yaml
rate-limiter:
  enabled: true  # 是否启用，默认true
```

## 🏗️ 工作原理

- **令牌桶算法**：平滑限流，允许突发流量
- **AOP切面**：无侵入拦截，保持代码整洁
- **本地缓存**：高性能内存存储
- **24h黑名单**：恶意用户自动屏蔽

## 📊 性能特点

- 响应时间 < 1ms
- 内存占用极少
- 线程安全保证
- 自动清理过期数据

## 📁 项目结构

```
rate-limiter/
├── README.md                           # 项目说明文档
├── pom.xml                            # Maven 配置文件
└── src/main/
    ├── java/com/example/ratelimiter/
    │   ├── algorithm/TokenBucket.java      # 令牌桶算法实现
    │   ├── annotation/RateLimit.java       # 限流注解定义
    │   ├── aop/RateLimitAOP.java          # AOP切面逻辑
    │   ├── cache/LocalCache.java          # 本地缓存实现
    │   └── config/RateLimiterAutoConfiguration.java  # 自动配置类
    └── resources/META-INF/
        └── spring.factories               # Spring Boot 自动配置声明
```

## 📄 许可证

本项目基于 MIT 许可证 开源。

**适用版本**: Spring Boot 2.x+ | JDK 1.8+
