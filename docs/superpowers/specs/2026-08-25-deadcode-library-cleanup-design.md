# 设计：死代码清理 + 自研能力换库（方案 B）

**日期：** 2026-08-25  
**状态：** 已获口头同意，待规格审阅后进入实现计划  
**模块：** `jinbooks/` 单体（Spring Boot 4.1）

---

## 1. 背景与目标

仓库中残留大量 MaxKey 时代自研封装与未接线代码：自定义 Redis 客户端层、kaptcha 源码 fork、自研 TOTP（`HmacOTP`）、JWT MITRE 风格签名栈、零引用工具包，以及已合并后仍存在的旧多模块目录。

**目标（一次做完，方案 B）：**

1. 删除无用 / 炫技自实现与死代码  
2. 有成熟库则改用库，不保留平行自研栈  
3. 额外明确：删除自研 Redis；图形验证码与 Google Authenticator/TOTP 两边都清  

**非目标：**

- 不上 OAuth2 Resource Server（阶段 3 已定）  
- 不改前端 baseURL / 不批量改业务控制器路径  
- 不删除密码兼容编码器（旧哈希仍需可登录）  
- 不改会计业务规则  

---

## 2. 替换对照表

| 现状 | 处理 |
|------|------|
| `com.financial.cloud.persistence.redis.**` + 自研 `RedisConnectionFactory` / Statement / Pool|Cluster | **删除**；改用 `spring-boot-starter-data-redis` + `StringRedisTemplate`（默认 Lettuce） |
| `RedisCacheService` / `RedisSessionManager` / `RedisCongressService` / `RedisSecretKeyManager` / `RedisOtpTokenStore` / `RedisDiplexCache` | 改为依赖 `StringRedisTemplate`（或带 Jackson 的 `RedisTemplate`） |
| `RedisAutoConfiguration` + `spring.redis.*` | 启用 Boot Redis 自动配置；属性迁到 Boot 4 的 `spring.data.redis.*` |
| `src/.../com/google/code/kaptcha/**` fork | **删除**；仅依赖官方 `kaptcha`；`KaptchaAutoConfiguration` 只用库内 `DefaultKaptcha` |
| `HmacOTP` 及自研 TOTP 算法 | 改为 `com.warrenstrange:googleauth`（或行为等价的维护中库）；统一生成/校验入口 |
| 恒失败 / 无调用的 OTP stub（如 `CapOtpAuthn`） | **删除**（确认无引用后） |
| `com.financial.cloud.json` 自定义 Date 序列化（main 零接线） | **删除**；日期靠 `MvcAutoConfiguration` + `@JsonFormat` |
| `HttpRequestAdapter`、`uuid/`、`pretty/`、`nanoid/`、`ldap/`、QR wrapper、孤立 crypto util、无用 BeanUtils 链 | **删除**（含仅服务它们的 `org.maxkey` 测试） |
| 旧多模块目录 `jinbooks-core/`、`jinbooks-commons/`、`jinbooks-starter/`、`jinbooks-persistence/`、`jinbooks-web/`（若仍存在且不参与 Maven） | **删除** |
| 自研 JWT MITRE 签名/校验栈（仍被 `AuthJwtService` 等使用） | 迁到已有依赖 `nimbus-jose-jwt` |
| `SnowFlakeId` | Hutool `IdUtil.getSnowflake()`；可留极薄门面以保持 `WebContext.genId()` |
| `DateUtils` 活跃调用 | 迁 `java.time` / Hutool `DateUtil`；再删死方法 |
| 散落 `JsonMapper.builder().build()` | 注入共享 `JsonMapper` bean |
| `pom`：`mapstruct`、`simple-http` 等零引用依赖；裸 `jedis`（若改 Lettuce 后无直接使用） | 移除或改为 Boot Redis starter 传递 |

---

## 3. Redis 设计

### 3.1 删除

删除包：

- `com.financial.cloud.persistence.redis`（含 `connection`、`statement`、`RedisConfig`、`RedisConfigConsts`、`IRedis*`）

### 3.2 新建/改造

- 依赖：`spring-boot-starter-data-redis`  
- 配置：`application-jinbooks.properties` 中 `spring.redis.*` → `spring.data.redis.*`（host/port/password/timeout/lettuce pool）；同步文档与 env 示例  
- `MemCacheService` 的 Redis 实现：用 `StringRedisTemplate` 的 `opsForValue` + TTL  
- Session / Congress / SecretKey / OtpTokenStore / Diplex：同样基于 Template；序列化策略固定为 **JSON 字符串**（或 Spring 默认 String+JDK 二选一并写死一种），并在规格实现计划中注明  
- **兼容性：** 迁移后旧 Redis key **不保证可读**；接受一次性会话失效 / 清库前缀  

### 3.3 无 Redis 回退

保留现有 InMemory 实现（`InMemoryCacheService`、`InMemorySessionManager` 等）及按配置切换的 auto-config 逻辑；仅去掉对自研 ConnectionFactory 的依赖。

---

## 4. 图形验证码设计

- 删除 `src/main/java/com/google/code/kaptcha/**`  
- 保留 `KaptchaAutoConfiguration` + `/captcha` 端点契约（返回 `Message<ImageCaptcha>`）  
- 仅使用 Maven 坐标 `com.github.penggle:kaptcha`（或当前 pom 已锁定版本）中的标准 Producer  
- 若官方 jar 缺少 fork 中的 Noise/Ripple 效果：接受默认样式，或改用配置属性调外观；**不**再 vendoring 源码  

---

## 5. Google Authenticator / TOTP 设计

- 引入 `com.warrenstrange:googleauth`（实现计划锁定具体版本）  
- 替换 `HmacOTP` 的生成/校验；QR / `sharedSecret` / `TimeBasedDto` 流程对接库 API  
- 短信 / 邮件 OTP **不是** Authenticator：保留业务发送逻辑，token 存取改走新 Redis/内存 store，不自研 HMAC 算法  
- 删除确认无引用的 stub：`CapOtpAuthn`、`RsaOtpAuthn` 等（实现前 grep）  

---

## 6. JWT / 日期 / ID / JSON

- **JWT：** `AuthJwtService` / refresh / signing service 改为 Nimbus `JWSSigner`/`JWSVerifier`/`SignedJWT`；删除不再使用的 MITRE 风格实现类  
- **DateUtils：** 按调用点迁移；保留临时桥接方法仅当编译阻塞，最终目标删除大段死代码  
- **Snowflake：** Hutool；保证 ID 长度/类型与现库字段兼容  
- **JsonMapper：** 单一 Spring bean；业务与拦截器禁止 `JsonMapper.builder().build()`  

---

## 7. 实施顺序（仍一次交付，但内部有序）

同一分支内按依赖顺序改，最终一次验收：

1. 删除旧多模块目录与确认零引用死包（含测试）  
2. Redis → Spring Data Redis；改配置；修所有消费者  
3. Kaptcha fork 删除 + 配置回归  
4. TOTP → googleauth  
5. JsonMapper 统一 + 无用依赖清理  
6. DateUtils / Snowflake  
7. JWT → Nimbus  
8. 全量 `package` + 冒烟（health、captcha、login、refresh；有 Redis 时再测 session）  

失败时优先回滚 JWT / Redis 相关提交粒度（实现计划按可回滚任务切分）。

---

## 8. 验收标准

- [ ] `.\mvnw.cmd -DskipTests package` 成功  
- [ ] 启动成功；`/jinbooks-api/actuator/health` → 200  
- [ ] `/captcha`、`/login/get` 匿名可用；图形码可出图  
- [ ] 账号密码登录成功；token refresh 成功  
- [ ] 启用 TOTP 时，库生成的码可校验（若环境可测）  
- [ ] 配置 Redis 时，缓存/会话读写正常；无 Redis 时 InMemory 仍可启动（若原本支持）  
- [ ] 仓库中不存在 `com.financial.cloud.persistence.redis` 与 `com.google.code.kaptcha` 源码包  
- [ ] `pom` 含 `spring-boot-starter-data-redis` 与 googleauth；无 mapstruct/simple-http（若确认零引用）  

---

## 9. 风险与缓解

| 风险 | 缓解 |
|------|------|
| Redis 序列化变更导致旧 key 失效 | 文档说明；部署时清相关 key / 接受重新登录 |
| `spring.redis` → `spring.data.redis` 漏改 | 全局 grep 配置与 compose/README |
| JWT 迁移破坏登录 | 实现后强制登录+refresh 冒烟；JWT 任务可单独回滚 |
| Kaptcha 外观变化 | 接受；仅保证接口契约 |
| googleauth 与旧 `sharedSecret` 编码不一致 | 对照现有 secret 存储格式（Base32 等）做兼容测试 |
| 误删仍被反射/配置引用的类 | 删除前全仓 grep；编译为门禁 |

---

## 10. 决策记录

- 推进方式：**B 一次清完**（内部有序，统一验收）  
- Google 验证码：**图形码 + TOTP 都清**  
- Redis：**删除自研层，改 Spring Data Redis**  
