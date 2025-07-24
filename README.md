# Rate Limiter Spring Boot Starter

一个**轻量化**的限流组件依赖库，基于 AOP 切面编程实现，采用令牌桶算法和黑名单机制。

## ✨ 特性

- 🎯 **轻量化设计**：零外部依赖，核心代码精简，不增加项目负担
- 🚀 **开箱即用**：自动配置，引入依赖即可使用，无需任何额外配置
- 📦 **高度复用**：作为 jar 包依赖，可快速集成到多个项目中
- 🔧 **易于维护**：独立的组件生命周期，便于版本管理和升级
- 🛡️ **智能防护**：令牌桶算法 + 黑名单机制，双重保护应用安全

## 🛠️ 快速开始

### 1. 添加依赖

#### Maven

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle

```gradle
implementation 'com.example:rate-limiter-spring-boot-starter:1.0.0'
```

### 2. 直接使用

**无需任何配置**！组件支持 Spring Boot 自动配置，引入依赖后即可直接使用。

在需要限流的方法上添加 `@RateLimit` 注解：

```java
@RateLimit(
    key = "userId",           // 限流标识（从请求参数中提取）
    fallback = "drawError",   // 限流时的回调方法
    qps = 1.0,               // 每秒允许请求次数
    blacklistCount = 1       // 黑名单触发次数
)
@PostMapping("/draw")
public Response<String> draw(@RequestBody DrawRequest request) {
    // 业务逻辑
    return success();
}

// 限流回调方法
public Response<String> drawError(@RequestBody DrawRequest request) {
    return error("访问限流拦截");
}
```

## 📚 核心组件

### @RateLimit 注解参数

| 参数           | 类型   | 默认值 | 说明                               |
| -------------- | ------ | ------ | ---------------------------------- |
| key            | String | "all"  | 限流标识字段，支持从请求参数中提取 |
| qps            | double | -      | 每秒允许请求次数（必填）           |
| blacklistCount | int    | 0      | 黑名单触发次数，0 表示不启用黑名单 |
| fallback       | String | -      | 限流后的回调方法名（必填）         |

### 限流策略

- **全局限流**：`key = "all"`，所有请求共享限流配额
- **用户级限流**：`key = "userId"`，按用户 ID 独立限流
- **IP 级限流**：`key = "clientIp"`，按 IP 地址独立限流
- **自定义限流**：`key = "自定义字段"`，按指定字段限流

### 黑名单机制

当用户触发限流次数超过 `blacklistCount` 时，该用户将被加入 24 小时黑名单，期间所有请求都会被拒绝。

## 🎯 使用示例

### 示例 1：用户抽奖限流

```java
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @RateLimit(
        key = "userId",
        fallback = "drawError",
        qps = 1.0,
        blacklistCount = 1
    )
    @PostMapping("/draw")
    public Response<DrawResult> draw(@RequestBody DrawRequest request) {
        // 抽奖业务逻辑
        return Response.success(drawResult);
    }

    public Response<DrawResult> drawError(@RequestBody DrawRequest request) {
        return Response.error("抽奖过于频繁，请稍后再试");
    }
}
```

### 示例 2：全局 API 限流

```java
@RateLimit(
    key = "all",
    fallback = "globalError",
    qps = 100.0
)
@GetMapping("/api/data")
public Response<Data> getData(@RequestParam String id) {
    // 获取数据逻辑
    return Response.success(data);
}

public Response<Data> globalError(@RequestParam String id) {
    return Response.error("系统繁忙，请稍后再试");
}
```

### 示例 3：IP 级限流

```java
@RateLimit(
    key = "all",  // 可扩展为获取真实IP
    fallback = "ipLimitError",
    qps = 10.0,
    blacklistCount = 5
)
@PostMapping("/comment")
public Response<String> addComment(@RequestBody Comment comment) {
    // 评论业务逻辑
    return Response.success("评论成功");
}

public Response<String> ipLimitError(@RequestBody Comment comment) {
    return Response.error("评论过于频繁，请稍后再试");
}
```

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     业务项目                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────┐  │
│  │   Controller    │───▶│   AOP Proxy     │───▶│Business │  │
│  └─────────────────┘    └─────────────────┘    └─────────┘  │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
    ┌─────────────────────────────────────────────────────────┐
    │              Rate Limiter 组件                         │
    │         ┌─────────────────┐                             │
    │         │   RateLimitAOP  │                             │
    │         └─────────────────┘                             │
    │                 │                                       │
    │   ┌─────────────┼─────────────┐                         │
    │   ▼             ▼             ▼                         │
    │ ┌─────────┐ ┌─────────┐ ┌─────────┐                     │
    │ │TokenBuc │ │LocalCac │ │Blacklis │                     │
    │ │(令牌桶) │ │(本地缓存)│ │(24h)   │                     │
    │ └─────────┘ └─────────┘ └─────────┘                     │
    └─────────────────────────────────────────────────────────┘
```

## 🧪 测试示例

### 启动测试项目

```bash
# 克隆项目
git clone https://github.com/yourkin666/rate-limiter.git
cd rate-limiter

# 编译安装
mvn clean install

# 创建测试项目并启动
# (需要创建示例项目或在现有项目中引入依赖)
```

### API 测试

**抽奖接口测试**：

```bash
curl -X POST http://localhost:8080/api/activity/draw \
  -H "Content-Type: application/json" \
  -d '{"userId":"user001","activityId":"ACT001"}'
```

**快速触发限流**：

```bash
# 连续调用多次，观察限流效果
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/activity/draw \
    -H "Content-Type: application/json" \
    -d '{"userId":"user001","activityId":"ACT001"}'
  echo ""
done
```

## 📊 监控和日志

组件提供详细的日志输出：

```
2024-12-25 10:30:15.123 [http-nio-8080-exec-1] INFO  RateLimitAOP - 限流标识：user001
2024-12-25 10:30:15.456 [http-nio-8080-exec-2] INFO  RateLimitAOP - 限流-超频次拦截：user001
2024-12-25 10:30:16.789 [http-nio-8080-exec-3] INFO  RateLimitAOP - 限流-黑名单拦截(24h)：user001
```

## ⚙️ 配置参数

在 `application.yml` 中可以配置：

```yaml
# 限流组件配置
rate-limiter:
  enabled: true # 是否启用限流组件，默认true

# 日志配置
logging:
  level:
    com.example.ratelimiter: INFO
```

## 🔧 最佳实践

1. **合理设置 QPS**：根据系统容量和业务需求设置合适的限流阈值
2. **黑名单策略**：谨慎使用黑名单功能，避免误判正常用户
3. **错误处理**：提供友好的限流提示信息，保证用户体验
4. **监控告警**：监控限流触发频率，及时调整策略
5. **回调方法**：确保 fallback 方法与原方法具有相同的参数签名

## 🚀 性能特点

- **高性能**：基于本地内存缓存，响应时间 < 1ms
- **低开销**：核心代码 < 10KB，内存占用极少
- **线程安全**：使用 ConcurrentHashMap 和同步机制保证并发安全
- **自动清理**：过期数据自动清理，避免内存泄漏
- **零依赖**：除 Spring Boot 外无其他外部依赖

## 🤔 常见问题

### Q: 如何禁用限流功能？

A: 在配置文件中设置 `rate-limiter.enabled=false`

### Q: 支持分布式限流吗？

A: 当前版本基于本地缓存，如需分布式限流建议使用 Redis 等外部存储

### Q: 黑名单数据会持久化吗？

A: 黑名单基于内存缓存，应用重启后会清空

### Q: 可以自定义令牌桶算法参数吗？

A: 当前版本令牌桶参数固定，后续版本会开放更多自定义选项

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**组件版本**: v1.0.0  
**适用框架**: Spring Boot 2.x+  
**JDK 版本**: 1.8+  
**作者**: yourkin666  
**GitHub**: https://github.com/yourkin666/rate-limiter
