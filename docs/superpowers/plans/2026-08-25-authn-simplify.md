# Authn Simplify (Bearer JWT + Interceptor) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Slim authn to login + captcha + Bearer JWT interceptor + refresh + logout (blacklist), without CAS/Congress/SM2/Session dual-track.

**Architecture:** Keep Nimbus HS512 access/refresh JWTs and `PermissionInterceptor`. Authenticate by validating JWT + memory blacklist only; rebuild `Authentication`/`UserInfo` from JWT claims (load roles from DB when needed). Delete CAS, Congress, SecretKey, and SessionManager login coupling.

**Tech Stack:** Spring Boot 4.1, Java 17, Nimbus JOSE+JWT, Caffeine, existing `PasswordEncoder` / captcha.

## Global Constraints

- Keep API paths: `/login/get`, `/captcha`, `/login/signin`, `/auth/token/refresh`, `/logout`
- Keep `AuthJwt` fields the frontend reads: `token`, `refresh_token`, `type`, `id`, `username`, `displayName`, `authorities`
- Login password is plaintext (no SM2 decrypt)
- `/login/get` must not require SecretKey; omit or null `secretKey`/`secretPublicKey`
- Do not git commit unless the user explicitly asks
- Active Maven module is `jinbooks/` (not legacy multi-module trees)
- Compile verify: `cd financial-cloud` then Maven wrapper `-DskipTests compile`

---

## File map

| Path | Responsibility |
|------|----------------|
| `authn/jwt/TokenBlacklistService.java` (create) | Caffeine blacklist of JWT `jti` until expiry |
| `authn/web/AuthorizationUtils.java` | Bearer-only auth; no SessionManager |
| `authn/web/interceptor/PermissionInterceptor.java` | Call simplified authenticate |
| `authn/provider/AbstractAuthenticationProvider.java` | Stop persisting SessionManager on login |
| `controller/auth/LoginController.java` | Drop SecretKey from `/login/get` |
| `controller/auth/LogoutController.java` | Blacklist jti instead of session terminate |
| `authn/web/AuthTokenRefreshPoint.java` | Blacklist old refresh on rotate |
| Delete: `authn/support/cas/**`, `congress/**`, `secretkey/**`, Trusted provider, SecretKeyEndpoint, CAS autoconfig | Dead paths |
| `autoconfigure/TokenAutoConfiguration.java` | No Congress |
| `autoconfigure/SessionAutoConfiguration.java` | Drop secretKeyManager bean; SessionManager only if still needed by admin UIs — prefer remove login dependency first |
| `autoconfigure/CasAuthnAutoConfiguration.java` | Delete + remove from AutoConfiguration.imports |

---

### Task 1: Token blacklist service

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/authn/jwt/TokenBlacklistService.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/autoconfigure/TokenAutoConfiguration.java` — register `@Bean TokenBlacklistService`
- Test: `financial-cloud/src/test/java/com/financial/cloud/authn/jwt/TokenBlacklistServiceTest.java`

**Interfaces:**
- Produces: `void revoke(String jti, long ttlSeconds)`, `boolean isRevoked(String jti)`

- [ ] **Step 1: Write failing test**

```java
package com.financial.cloud.authn.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {
    @Test
    void revokeThenIsRevoked() {
        TokenBlacklistService svc = new TokenBlacklistService();
        assertFalse(svc.isRevoked("jti-1"));
        svc.revoke("jti-1", 60);
        assertTrue(svc.isRevoked("jti-1"));
    }

    @Test
    void blankJtiIsNotRevoked() {
        TokenBlacklistService svc = new TokenBlacklistService();
        assertFalse(svc.isRevoked(null));
        assertFalse(svc.isRevoked(""));
    }
}
```

- [ ] **Step 2: Run test — expect compile/fail missing class**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
Set-Location C:\Users\Administrator\Projects\jinbooks\jinbooks
& "$env:JAVA_HOME\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=$pwd" -classpath ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain -Dtest=TokenBlacklistServiceTest test
```

Expected: FAIL (class not found / compile error)

- [ ] **Step 3: Implement**

```java
package com.financial.cloud.authn.jwt;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class TokenBlacklistService {
    private final Cache<String, Boolean> revoked = Caffeine.newBuilder()
            .expireAfterWrite(48, TimeUnit.HOURS)
            .maximumSize(200_000)
            .build();

    public void revoke(String jti, long ttlSeconds) {
        if (StringUtils.isBlank(jti)) {
            return;
        }
        long ttl = Math.max(ttlSeconds, 1);
        revoked.policy().expireVariably().ifPresentOrElse(
                policy -> policy.put(jti, Boolean.TRUE, ttl, TimeUnit.SECONDS),
                () -> revoked.put(jti, Boolean.TRUE));
    }

    public boolean isRevoked(String jti) {
        if (StringUtils.isBlank(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(revoked.getIfPresent(jti));
    }
}
```

If `expireVariably` is awkward on this Caffeine version, use fixed `expireAfterWrite(48, HOURS)` and `put(jti, TRUE)` only (acceptable for logout).

- [ ] **Step 4: Register bean in `TokenAutoConfiguration`**

```java
@Bean
TokenBlacklistService tokenBlacklistService() {
    return new TokenBlacklistService();
}
```

- [ ] **Step 5: Re-run test — expect PASS**

---

### Task 2: AuthorizationUtils — JWT only + blacklist

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/AuthorizationUtils.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/interceptor/PermissionInterceptor.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/permissions/OpenFuncListController.java` (same authenticate signature)
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/jwt/service/AuthTokenService.java` — inject blacklist into `validateJwtToken` OR check in AuthorizationUtils

**Interfaces:**
- Consumes: `TokenBlacklistService.isRevoked(jti)`, `AuthTokenService.validateJwtToken`, `resolve(JWTClaimsSet)`
- Produces: `authenticate(request, authTokenService, tokenBlacklistService)` — no `SessionManager`
- Builds `UsernamePasswordAuthenticationToken` with `SignedPrincipal` from claims: `sub`→username, `ConstsJwt.USER_ID`, `ConstsJwt.INST_ID`, `ConstsJwt.LOCALE`; `jti` as sessionId on principal/UserInfo

- [ ] **Step 1: Change `AuthTokenService.validateJwtToken` usage**

In `AuthorizationUtils.doJwtAuthenticate`:
1. If blank/undefined token → clear and return  
2. If `!authTokenService.validateJwtToken(authorization)` → clear  
3. Parse claims; if `tokenBlacklistService.isRevoked(claims.getJWTID())` → clear  
4. Build `UserInfo` minimally:

```java
UserInfo user = new UserInfo();
user.setId(claims.getStringClaim(ConstsJwt.USER_ID));
user.setUsername(claims.getSubject());
user.setBookId(claims.getStringClaim(ConstsJwt.INST_ID));
user.setLocale(claims.getStringClaim(ConstsJwt.LOCALE));
user.setSessionId(claims.getJWTID());
```

5. `SignedPrincipal principal = new SignedPrincipal(user);` — if constructor requires `Session`, create a transient `Session` with id=`jti` only (do not store in SessionManager)  
6. Wrap authorities: empty list OR call `LoginService`/`grantAuthority` if easy; empty is OK if APIs reload roles  
7. `setAuthentication(request, authenticationToken)`

- [ ] **Step 2: Update `authenticate` signature** — remove SessionManager; remove congress parameter/cookie branches (Bearer header only is enough; cookie `congress` may stay as fallback reading JWT string if frontend still sends it — keep cookie/param as alternate Bearer transport, not CongressService)

- [ ] **Step 3: Update `PermissionInterceptor`**

```java
@Autowired TokenBlacklistService tokenBlacklistService;
// remove SessionManager
AuthorizationUtils.authenticate(request, authTokenService, tokenBlacklistService);
```

- [ ] **Step 4: Fix all call sites** (grep `AuthorizationUtils.authenticate` and `doJwtAuthenticate`)

- [ ] **Step 5: Compile**

```powershell
# same java -jar maven-wrapper pattern as Task 1
... MavenWrapperMain -DskipTests compile
```

Expected: BUILD SUCCESS

---

### Task 3: Login without SessionManager / SecretKey

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/auth/LoginController.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/provider/AbstractAuthenticationProvider.java` (`createOnlineTicket`)
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/provider/impl/NormalAuthenticationProvider.java` — remove SM2 decrypt if present
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/dto/LoginConfigDto.java` — secret fields optional/nullable

- [ ] **Step 1: `/login/get`** — remove `SecretKeyManager` injection and:

```java
// delete:
// LoginSecretKey loginSecretKey = secretKeyManager.getSecretKey();
// conf.setSecretKey(...); conf.setSecretPublicKey(...);
```

Keep `state` via `authTokenService.genRandomJwt()`.

- [ ] **Step 2: `createOnlineTicket`** — still build `Session` + `SignedPrincipal` + authorities for JWT claims, but **remove** `sessionManager.create(...)` and DB online-session writes if they are only for dual-track. Keep HttpSession attribute for same-request `@CurrentUser` if needed.

Locate and delete/comment lines like:
```java
sessionManager.create(session.getId(), session);
// sessionListService insert online ...
```

- [ ] **Step 3: Password** — ensure login uses plaintext `credential.getPassword()` with existing PasswordEncoder match; remove any `secretKeyManager.decrypt(...)` branch.

- [ ] **Step 4: Compile** — BUILD SUCCESS

---

### Task 4: Logout + refresh blacklist

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/auth/LogoutController.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/AuthTokenRefreshPoint.java`

- [ ] **Step 1: Logout**

```java
@GetMapping("/logout")
public Message<String> logout(HttpServletRequest request,
        @CurrentUser UserInfo currentUser,
        @Autowired AuthTokenService authTokenService,
        @Autowired TokenBlacklistService blacklist) throws Exception {
    String bearer = AuthorizationHeaderUtils.resolveBearer(request);
    if (StringUtils.isNotBlank(bearer) && authTokenService.validateJwtToken(bearer)) {
        var claims = authTokenService.resolve(bearer);
        long ttl = Math.max(1, (claims.getExpirationTime().getTime() - System.currentTimeMillis()) / 1000);
        blacklist.revoke(claims.getJWTID(), ttl);
    }
    AuthorizationUtils.clearAuthentication();
    return new Message<>();
}
```

(Adapt field injection style to match existing controller.)

- [ ] **Step 2: Refresh** — after validating old refresh JWT and issuing new tokens, `blacklist.revoke(oldRefreshJti, remainingTtl)`.

- [ ] **Step 3: Compile**

---

### Task 5: Delete CAS / Congress / SecretKey / Trusted

**Files to delete (confirm zero references with grep first):**
- `authn/support/cas/**`
- `authn/congress/**`
- `authn/secretkey/**` (including `SM2Utils.java`, `LoginSecretKey.java` if unused)
- `authn/provider/impl/TrustedAuthenticationProvider.java`
- `authn/web/SecretKeyEndpoint.java`
- `autoconfigure/CasAuthnAutoConfiguration.java`

**Modify:**
- `AuthenticationProviderFactory` — only register Normal provider
- `AuthnProviderAutoConfiguration` — drop Trusted/CAS wiring
- `TokenAutoConfiguration` — only `InMemoryCongressService` removal; construct `AuthTokenService` without Congress if possible
- `SessionAutoConfiguration` — remove `secretKeyManager` bean
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — remove Cas line
- Grep `CongressService` / `createCongress` / `consumeCongress` in `AuthTokenService` — remove methods or no-op delete callers

- [ ] **Step 1: Grep and delete dead types**

```powershell
rg "CongressService|SecretKeyManager|TrustedAuthentication|CasTrust|SM2Utils|LoginSecretKey" financial-cloud/src/main/java
```

- [ ] **Step 2: Delete files + fix compile errors**

- [ ] **Step 3: If `AuthTokenService` still requires `CongressService`, change constructor to drop it**

- [ ] **Step 4: `mvn -DskipTests compile` SUCCESS**

---

### Task 6: Drop SessionManager from auth path (optional cleanup)

**Files:**
- Grep `SessionManager` usages under login/interceptor/AuthorizationUtils  
- Keep `SessionManager` only if `SessionController` admin UI still needs it; otherwise leave bean but unused by auth  
- Delete `SessionTimeoutScheduler` auth coupling if it only cleaned dual-track sessions  
- Remove `isRedis` leftovers already gone

- [ ] **Step 1: Grep `SessionManager` in `financial-cloud/src/main/java`**
- [ ] **Step 2: Remove auth-path injections; leave admin session list as follow-up if broken**
- [ ] **Step 3: Compile**

---

### Task 7: Smoke verification

- [ ] **Step 1: Compile + test-compile**

```powershell
... MavenWrapperMain -DskipTests compile
... MavenWrapperMain test-compile
```

- [ ] **Step 2: Manual smoke (if app can start)**  
  1. `GET /jinbooks-api/login/get` → has `state`, no required secret keys  
  2. `GET /captcha?state=` → image  
  3. `POST /login/signin` → `token` + `refresh_token`  
  4. `GET /users/currentUser` with `Authorization: Bearer` → 200  
  5. `GET /logout` → 200; same Bearer → 401  
  6. refresh endpoint still works once  

- [ ] **Step 3: Update spec status** in `docs/superpowers/specs/2026-08-25-authn-simplify-design.md` to `已完成` when done

---

## Spec coverage check

| Spec item | Task |
|-----------|------|
| Bearer JWT interceptor | Task 2 |
| Captcha + state kept | Task 3 (unchanged captcha path) |
| Refresh kept + blacklist old | Task 4 |
| Logout blacklist | Task 4 |
| Remove CAS/Congress/SecretKey | Task 5 |
| Remove Session dual-track login | Task 3 + 6 |
| Plaintext password | Task 3 |
| Compile / smoke | Task 7 |

## Placeholder scan

No TBD steps; Caffeine variable TTL has a fixed-TTL fallback noted in Task 1.
