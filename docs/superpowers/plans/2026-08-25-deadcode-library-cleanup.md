# Deadcode Cleanup + Library Replacement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete unused/self-built MaxKey-era code and replace custom Redis, kaptcha fork, TOTP, and leftover JWT MITRE layers with Spring Data Redis, official kaptcha, googleauth, and Nimbus-only JWT.

**Architecture:** One delivery branch with ordered, independently committable tasks. Redis consumers switch to `StringRedisTemplate` with JSON string values. Auth JWT already uses Nimbus via `Hmac512Service` — delete the unused MITRE signer/encryption stack. Captcha keeps `/captcha` contract; TOTP uses `com.warrenstrange:googleauth`.

**Tech Stack:** Spring Boot 4.1, `spring-boot-starter-data-redis` (Lettuce), `com.github.penggle:kaptcha:2.3.2`, `com.warrenstrange:googleauth:1.5.0`, `nimbus-jose-jwt` (existing), Hutool (existing), Jackson 3 `JsonMapper`.

**Spec:** `docs/superpowers/specs/2026-08-25-deadcode-library-cleanup-design.md`

## Global Constraints

- Do not introduce OAuth2 Resource Server.
- Do not change frontend baseURL or bulk controller mappings.
- Do not remove password compatibility encoders.
- Do not change accounting business rules.
- Accept one-time Redis session invalidation after migration.
- Work under `jinbooks/` (+ docs). Large dirty tree may exist — stage only files for the current task.
- Build: `cd jinbooks` then `.\mvnw.cmd -DskipTests compile` / `package` (set `JAVA_HOME`; quote `-D` in PowerShell).
- Context path remains `/jinbooks-api`.

---

## File Structure (target)

| Path | Role after cleanup |
|------|-------------------|
| `pom.xml` | Add data-redis + googleauth; remove jedis/mapstruct/simple-http if unused |
| `application-jinbooks.properties` | `spring.data.redis.*` |
| `autoconfigure/RedisAutoConfiguration.java` | Thin: enable Boot Redis / optional beans only |
| `persistence/cache/RedisCacheService.java` | `StringRedisTemplate` + JsonMapper |
| `authn/session/impl/RedisSessionManager.java` (and SessionManagerImpl wiring) | Template-based |
| `authn/congress/RedisCongressService.java` | Template-based |
| `authn/secretkey/impl/RedisSecretKeyManager.java` | Template-based |
| `password/onetimepwd/token/RedisOtpTokenStore.java` | Template-based |
| `persistence/cache/diplex/RedisDiplexCache.java` | Template-based or delete if unused |
| `password/onetimepwd/TotpService.java` (new) | googleauth wrapper |
| `resources/kaptcha.properties` | Official Default* impl class names |
| **Deleted:** `persistence/redis/**`, `com/google/code/kaptcha/**`, dead util packs, MITRE jwt signer/encryption (keep `Hmac512Service`), old multi-module dirs |

---

### Task 1: Delete old modules + confirmed dead packages

**Files:**
- Delete directories (if present under `jinbooks/`): `jinbooks-core/`, `jinbooks-commons/`, `jinbooks-starter/`, `jinbooks-persistence/`, `jinbooks-web/`
- Delete packages under `jinbooks/src/main/java` (after grep confirms no main callers):
  - `com/jinbooks/json/` (custom serializers)
  - `com/jinbooks/web/HttpRequestAdapter.java`
  - `com/jinbooks/uuid/`
  - `com/jinbooks/pretty/`
  - `com/jinbooks/nanoid/`
  - `com/jinbooks/ldap/`
  - `com/jinbooks/util/QRCode.java`, `QRCodeUtils.java`, `QRCodeConfig.java`
  - Unused util leftovers listed in spec (§2): `IdSequence`, `MacAddress`, `EthernetAddress`, `HttpEncoder`, `HttpsTrusts`, `StreamUtils`, `Preconditions`, unused BeanUtil chain if grep-clean
  - Isolated crypto: `SM4Utils`, `Base32Utils`, `Md5Sum`, `CertSigner`, `CertCrypto` if grep-clean
- Delete matching tests under `src/test/java/org/maxkey/` that only cover deleted code
- Do **not** delete Redis or kaptcha yet (Tasks 2–3)

**Interfaces:**
- Consumes: none
- Produces: smaller tree; compile still green for remaining code

- [ ] **Step 1: Grep gate before each deletion**

For each candidate package/class, run ripgrep from repo root. Example:

```powershell
cd C:\Users\Administrator\Projects\jinbooks
rg "HttpRequestAdapter|com\.jinbooks\.uuid|com\.jinbooks\.pretty|com\.jinbooks\.nanoid|com\.jinbooks\.ldap|JsonDateSerializer|QRCodeUtils" -g "*.java"
```

Only delete when **main** has zero imports (tests-only is OK to delete with the code).

- [ ] **Step 2: Remove old multi-module directories**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
Remove-Item -Recurse -Force jinbooks-core,jinbooks-commons,jinbooks-starter,jinbooks-persistence,jinbooks-web -ErrorAction SilentlyContinue
```

Confirm root/`jinbooks/pom.xml` has no `<modules>` referencing them.

- [ ] **Step 3: Delete dead Java packages/files + orphan tests**

Use `git rm -r` for tracked paths. Example:

```powershell
git rm -r jinbooks/src/main/java/com/jinbooks/json
git rm jinbooks/src/main/java/com/jinbooks/web/HttpRequestAdapter.java
# ... remaining list after grep
```

- [ ] **Step 4: Compile**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add -A
git commit -m "chore: remove unused MaxKey packages and old multi-module trees"
```

Only include Task 1 deletions (avoid staging Redis/kaptcha WIP).

---

### Task 2: Redis → Spring Data Redis

**Files:**
- Modify: `jinbooks/pom.xml`
- Modify: `jinbooks/src/main/resources/application-jinbooks.properties`
- Rewrite: `jinbooks/src/main/java/com/jinbooks/autoconfigure/RedisAutoConfiguration.java`
- Rewrite: `RedisCacheService`, `RedisOtpTokenStore`, `RedisSecretKeyManager`, `RedisCongressService`, `RedisSessionManager` / `SessionManagerImpl`, `RedisDiplexCache` (or delete if unused)
- Modify: `SessionAutoConfiguration`, `TokenAutoConfiguration`, `OneTimePasswordAutoConfiguration`, any other `RedisConnectionFactory` injectors
- Delete: entire `jinbooks/src/main/java/com/jinbooks/persistence/redis/`
- Remove `RedisConnectionFactory` from `AutoConfiguration.imports` only if class removed; keep a Boot-friendly Redis auto-config entry
- Create: `jinbooks/src/test/java/com/jinbooks/persistence/cache/RedisCacheServiceTest.java` (unit test with mocked `StringRedisTemplate` **or** skip if no easy mock — then compile + smoke in Task 8)
- Update README/docker notes if they document `spring.redis.*`

**Interfaces:**
- Consumes: `ApplicationConfig.isCachedRedis()`, InMemory fallbacks
- Produces: All Redis-backed services take `StringRedisTemplate` (+ shared `JsonMapper` for object values). No `com.jinbooks.persistence.redis` types remain.

- [ ] **Step 1: Add dependency; remove direct jedis if unused after**

In `pom.xml` dependencies:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Remove standalone `redis.clients:jedis` dependencyManagement/dependency entries when no direct Jedis API remains.

- [ ] **Step 2: Migrate properties**

Replace block in `application-jinbooks.properties`:

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PWD:}
spring.data.redis.timeout=10s
spring.data.redis.lettuce.pool.max-active=200
spring.data.redis.lettuce.pool.max-idle=200
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=1s
```

Remove old `spring.redis.*` keys (and empty `cluster.nodes` unless cluster still required — default single-node).

- [ ] **Step 3: Rewrite RedisCacheService**

Use JSON strings (accept breaking old keys):

```java
public class RedisCacheService implements MemCacheService {
    public static final String PREFIX = "jb:momentary:%s:%s";
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private int validitySeconds = 60 * 5;

    public RedisCacheService(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void put(String sessionId, String name, Object value) {
        String key = PREFIX.formatted(sessionId, name);
        try {
            redis.opsForValue().set(key, jsonMapper.writeValueAsString(value),
                    Duration.ofSeconds(validitySeconds));
        } catch (Exception e) {
            throw new IllegalStateException("redis put failed", e);
        }
    }

    @Override
    public Object get(String sessionId, String name) {
        String json = redis.opsForValue().get(PREFIX.formatted(sessionId, name));
        if (json == null) return null;
        try {
            return jsonMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("redis get failed", e);
        }
    }
    // remove similarly
}
```

Adapt other Redis* classes the same way (key prefixes may keep `mxk:` or rename to `jb:` — prefer `jb:` and document invalidation).

- [ ] **Step 4: Rewire auto-configurations**

`SessionAutoConfiguration.sessionManager(...)` and `secretKeyManager(...)`: inject `ObjectProvider<StringRedisTemplate>` or `StringRedisTemplate` only when `isCachedRedis()`; pass `null`/InMemory otherwise. **Never** require Redis beans when `jinbooks.server.cached=0` (InMemory).

Pattern:

```java
@Bean
SessionManager sessionManager(..., ObjectProvider<StringRedisTemplate> redis,
                               ApplicationConfig applicationConfig, ...) {
    StringRedisTemplate template = applicationConfig.isCachedRedis()
            ? redis.getIfAvailable() : null;
    return new SessionManagerImpl(..., template, ...);
}
```

Ensure Boot does not fail startup without Redis when InMemory mode: use `@ConditionalOnProperty` / `ObjectProvider`, or set `spring.data.redis` only when needed. If Boot auto-config fails without a broker, add:

```properties
# when using in-memory cache in local profile, exclude Redis auto-config OR use a testcontainer
```

Preferred: `jinbooks.server.cached=0` default stays InMemory; Redis auto-config `@AutoConfigureAfter` + `@ConditionalOnProperty(name="jinbooks.server.cached", havingValue="1")` for Redis-backed beans; disable Redis health (`management.health.redis.enabled=false` already).

- [ ] **Step 5: Delete `persistence/redis` package**

```powershell
git rm -r jinbooks/src/main/java/com/jinbooks/persistence/redis
```

Fix all compile errors until zero references to `RedisConnectionFactory` / `IRedisStatement`.

- [ ] **Step 6: Compile**

```powershell
.\mvnw.cmd -DskipTests compile
```

- [ ] **Step 7: Commit**

```powershell
git commit -m "refactor: replace custom Redis stack with Spring Data Redis"
```

---

### Task 3: Remove kaptcha fork; use library defaults

**Files:**
- Delete: `jinbooks/src/main/java/com/google/code/kaptcha/**`
- Modify: `jinbooks/src/main/resources/kaptcha.properties`
- Keep: `KaptchaAutoConfiguration`, `ImageCaptchaEndpoint`, `web/kaptcha/ImageCaptcha.java`, `CaptchaContent.java`

**Interfaces:**
- Consumes: official kaptcha `Producer` / `DefaultKaptcha`
- Produces: `/captcha` still returns `Message` with image payload

- [ ] **Step 1: Rewrite kaptcha.properties to stock implementations**

```properties
kaptcha.border=no
kaptcha.image.width=100
kaptcha.image.height=50
kaptcha.obscurificator.impl=com.google.code.kaptcha.impl.WaterRipple
kaptcha.noise.impl=com.google.code.kaptcha.impl.DefaultNoise
kaptcha.textproducer.impl=com.google.code.kaptcha.text.impl.DefaultTextCreator
kaptcha.word.impl=com.google.code.kaptcha.text.impl.DefaultWordRenderer
kaptcha.textproducer.char.length=4
kaptcha.textproducer.font.names=Arial,Courier
kaptcha.textproducer.font.size=40
kaptcha.background.clear.from=white
kaptcha.background.clear.to=white
```

(Adjust only if existing non-fork keys must remain — strip all references to `Ripple`, `LightNoise`, `RandomColorWordRenderer`, `UniqueTextCreator`.)

- [ ] **Step 2: Delete fork sources**

```powershell
git rm -r jinbooks/src/main/java/com/google
```

- [ ] **Step 3: Compile + optional quick check that `KaptchaAutoConfiguration` still builds `Producer` bean**

```powershell
.\mvnw.cmd -DskipTests compile
```

- [ ] **Step 4: Commit**

```powershell
git commit -m "chore: drop vendored kaptcha fork; use library defaults"
```

---

### Task 4: TOTP → googleauth; remove dead OTP stubs

**Files:**
- Modify: `pom.xml` — add googleauth
- Create: `jinbooks/src/main/java/com/jinbooks/password/onetimepwd/TotpService.java`
- Create: `jinbooks/src/test/java/com/jinbooks/password/onetimepwd/TotpServiceTest.java`
- Delete: `algorithm/HmacOTP.java`
- Delete stubs if unused: `CapOtpAuthn`, `RsaOtpAuthn`, `MessageQueueOtpAuthn` (grep first)
- Wire `TotpService` where `sharedSecret` / TFA validation needs TOTP (search `tfaOtpAuthn`, `TIMEBASED`, `sharedSecret` usages). If no production caller today, still add `TotpService` + test and delete `HmacOTP`; document for future TFA wiring.

**Interfaces:**
- Produces:

```java
public class TotpService {
  public String createSecret();
  public String getOtpAuthUrl(String issuer, String account, String secret);
  public boolean verify(String secret, int code);
}
```

Using `GoogleAuthenticator` / `GoogleAuthenticatorKey` from `com.warrenstrange.googleauth`.

- [ ] **Step 1: Add dependency**

```xml
<dependency>
  <groupId>com.warrenstrange</groupId>
  <artifactId>googleauth</artifactId>
  <version>1.5.0</version>
</dependency>
```

- [ ] **Step 2: Write failing test**

```java
class TotpServiceTest {
  @Test
  void generateAndVerify() {
    TotpService svc = new TotpService();
    String secret = svc.createSecret();
    GoogleAuthenticator ga = new GoogleAuthenticator();
    int code = ga.getTotpPassword(secret);
    assertTrue(svc.verify(secret, code));
  }
}
```

- [ ] **Step 3: Implement TotpService; delete HmacOTP; remove stubs after grep**

- [ ] **Step 4: Run test**

```powershell
.\mvnw.cmd "-Dtest=TotpServiceTest" test
```

- [ ] **Step 5: Commit**

```powershell
git commit -m "feat: replace custom HmacOTP with googleauth TotpService"
```

---

### Task 5: Shared JsonMapper + remove unused Maven deps

**Files:**
- Ensure `JsonMapper` `@Bean` exists (likely in `MvcAutoConfiguration` or new `JacksonAutoConfiguration`)
- Replace remaining `JsonMapper.builder().build()` in main (grep): `BookSubjectServiceImpl`, `StatementSubjectBalanceServiceImpl`, `JWKSetKeyStore`, etc.
- Remove from `pom.xml`: `mapstruct`, `simple-http` (and version props) after confirming zero references
- Fix `PageQuery.build()` dead `pageSize == null` branch if still present

**Interfaces:**
- Produces: single injectable `JsonMapper` used app-wide

- [ ] **Step 1: Grep ad-hoc mappers and mapstruct/simple-http**

```powershell
rg "JsonMapper\.builder\(\)\.build\(\)|new ObjectMapper\(" jinbooks/src/main/java -g "*.java"
rg "mapstruct|simple-http|org\.mapstruct" jinbooks -g "*.{java,xml}"
```

- [ ] **Step 2: Inject shared bean; remove dead deps; compile**

- [ ] **Step 3: Commit**

```powershell
git commit -m "refactor: centralize JsonMapper and drop unused Maven deps"
```

---

### Task 6: Snowflake → Hutool; trim DateUtils

**Files:**
- Modify: `SnowFlakeId` / `IdGenerator` / `WebContext.genId()` to use `cn.hutool.core.util.IdUtil.getSnowflake(workerId, datacenterId)` (worker ids from config or fixed `1,1`)
- Migrate hottest `DateUtils` call sites that are trivial; delete clearly unused methods after grep
- Do not boil-the-ocean every DateUtils caller in one day — minimum: delete unused private/public methods with zero references; migrate new code paths only if touching files anyway

**Interfaces:**
- Produces: IDs still fit DB string/long fields (verify length vs previous SnowFlake)

- [ ] **Step 1: Replace SnowFlake implementation body with Hutool; keep class name as façade if many callers**

- [ ] **Step 2: Grep-delete unused DateUtils methods; compile**

- [ ] **Step 3: Commit**

```powershell
git commit -m "refactor: use Hutool snowflake and trim dead DateUtils APIs"
```

---

### Task 7: Delete unused MITRE JWT signer/encryption stack

**Files:**
- Keep: `com.jinbooks.crypto.jwt.Hmac512Service` (already Nimbus; used by Auth*TokenService)
- Delete if still only self-referenced (confirmed earlier):  
  `crypto/jwt/signer/**`, `crypto/jwt/encryption/**`  
  including `DefaultJwtSigningAndValidationService`, `JWKSetCacheService`, `SymmetricCacheService`, builders, encryption services
- Keep `jose/keystore/JWKSetKeyStore` only if still referenced after deletions; otherwise delete or slim

**Interfaces:**
- Consumes: Auth path unchanged (`Hmac512Service` + `SignedJWT`)
- Produces: no MITRE-style unused JWT services

- [ ] **Step 1: Re-grep imports of signer/encryption packages from outside themselves**

```powershell
rg "crypto\.jwt\.signer|crypto\.jwt\.encryption" jinbooks/src/main/java -g "*.java"
```

- [ ] **Step 2: Delete unused tree; compile; login-critical classes must still compile**

- [ ] **Step 3: Commit**

```powershell
git commit -m "chore: remove unused MITRE-style JWT signer/encryption stack"
```

---

### Task 8: Package + smoke verification

**Files:** none unless smoke forces a tiny fix

- [ ] **Step 1: Package**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -DskipTests package
```

- [ ] **Step 2: Start jar (InMemory mode default)**

Confirm log: `Started JinBooksApplication`

- [ ] **Step 3: Smoke**

| Check | Expect |
|-------|--------|
| `GET /jinbooks-api/actuator/health` | 200 |
| `GET /jinbooks-api/captcha` | 200 + image data in Message |
| `GET /jinbooks-api/login/get` | not 401 |
| `GET /jinbooks-api/book/fetch` | 401 Message |
| Login + `/auth/token/refresh` if credentials known | success |

Optional Redis: set `jinbooks.server.cached=1` and Redis up; restart; login twice and confirm session works.

- [ ] **Step 4: Verify deletions**

```powershell
Test-Path jinbooks/src/main/java/com/jinbooks/persistence/redis
Test-Path jinbooks/src/main/java/com/google/code/kaptcha
```

Both must be `$false`.

- [ ] **Step 5: Write smoke notes to `.superpowers/sdd/cleanup-smoke-report.md` (do not need to commit). Commit only if code fixes were required.**

---

## Self-Review (plan vs spec)

| Spec item | Task |
|-----------|------|
| Delete old modules + dead packs | Task 1 |
| Redis → Spring Data Redis; delete custom layer | Task 2 |
| Kaptcha fork delete + official defaults | Task 3 |
| TOTP googleauth; both captcha types | Tasks 3–4 |
| JsonMapper unify; drop mapstruct/simple-http | Task 5 |
| DateUtils / Snowflake | Task 6 |
| JWT → Nimbus (auth already Nimbus; remove MITRE leftovers) | Task 7 |
| Package + smoke | Task 8 |
| No OAuth2 RS / no frontend baseURL / keep password encoders | Global constraints |

**Note:** Spec said “JWT migrate to Nimbus”; codebase auth path already uses Nimbus via `Hmac512Service`. Task 7 removes the unused parallel MITRE stack instead of rewriting working auth tokens.
