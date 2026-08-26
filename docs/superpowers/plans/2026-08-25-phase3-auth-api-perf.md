# Phase 3 Auth / API / Perf Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden default-deny JWT auth, unify 401/403 into `Message`, add `/api/v1` path rewrite, cap pagination, and remove voucher batch N+1 queries.

**Architecture:** Keep existing JWT + `PermissionInterceptor` + session model. Flip intercept to `/**` with an explicit public exclude list; write `Message` JSON on 401 instead of forwarding. Add a servlet Filter that strips `/api/v1` before MVC. Tighten `PageQuery` defaults/caps. Add `queryByIds` batch load in `VoucherServiceImpl` and use it in `submitBatch` / `audit` / `delete`.

**Tech Stack:** Spring Boot 4.1, Spring MVC interceptors/filters, Jackson 3 (`tools.jackson`), MyBatis-Plus, JUnit 5, Maven wrapper under `jinbooks/`.

**Spec:** `docs/superpowers/specs/2026-08-25-phase3-auth-api-perf-design.md`

## Global Constraints

- Do not introduce OAuth2 Resource Server in this wave.
- Do not change frontend `baseURL` or bulk-edit controller `@RequestMapping` paths.
- Keep `Message.SUCCESS = 0` and `Message.FAIL = 2`.
- Book business codes move to `51xxxx`; Orgs to `52xxxx`.
- `PageQuery` default page size = `20`; max = `100`.
- Work only under monorepo module `jinbooks/` (plus this docs tree). Do not commit secrets.
- Build from `jinbooks/`: `.\mvnw.cmd -DskipTests package` (Windows) or `./mvnw -DskipTests package`.
- Context path remains `/jinbooks-api`.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `financial-cloud/src/main/java/com/financial/cloud/entity/Message.java` | Add `UNAUTHORIZED=401`, `FORBIDDEN=403` |
| `financial-cloud/src/main/java/com/financial/cloud/authn/web/interceptor/PermissionInterceptor.java` | Write `Message` 401 JSON; no forward |
| `financial-cloud/src/main/java/com/financial/cloud/autoconfigure/FinancialCloudMvcConfig.java` | Intercept `/**` + public excludes |
| `financial-cloud/src/main/java/com/financial/cloud/authn/web/UnauthorizedEntryPoint.java` | Return `Message` 401 |
| `financial-cloud/src/main/java/com/financial/cloud/authn/web/RefusedPoint.java` | Return `Message` 403 |
| `financial-cloud/src/main/java/com/financial/cloud/enums/BookBusinessExceptionEnum.java` | Codes `510001+` |
| `financial-cloud/src/main/java/com/financial/cloud/enums/OrgsBusinessExceptionEnum.java` | Codes `520001+` |
| `financial-cloud/src/main/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilter.java` | Strip `/api/v1` prefix |
| `financial-cloud/src/main/java/com/financial/cloud/autoconfigure/ApiV1PathRewriteAutoConfiguration.java` | Register Filter bean |
| `financial-cloud/src/main/java/com/financial/cloud/entity/PageQuery.java` | Default 20, max 100 clamp |
| `financial-cloud/src/main/java/com/financial/cloud/persistence/service/impl/VoucherServiceImpl.java` | Batch `queryByIds`; fix N+1 |
| `financial-cloud/src/test/java/com/financial/cloud/entity/PageQueryTest.java` | Pagination unit tests |
| `financial-cloud/src/test/java/com/financial/cloud/entity/MessageAuthCodesTest.java` | Constant smoke tests |
| `financial-cloud/src/test/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilterTest.java` | Rewrite unit tests |
| `financial-cloud/src/test/java/com/financial/cloud/enums/BusinessExceptionCodeRangesTest.java` | Code-range assertions |

---

### Task 1: Message auth constants + PageQuery caps

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/entity/Message.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/entity/PageQuery.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/entity/MessageAuthCodesTest.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/entity/PageQueryTest.java`

**Interfaces:**
- Consumes: none
- Produces: `Message.UNAUTHORIZED` (`int 401`), `Message.FORBIDDEN` (`int 403`); `PageQuery.DEFAULT_PAGE_SIZE=20`, `PageQuery.MAX_PAGE_SIZE=100`; `PageQuery.build()` clamps size

- [ ] **Step 1: Write failing tests**

Create `MessageAuthCodesTest.java`:

```java
package com.financial.cloud.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageAuthCodesTest {
    @Test
    void unauthorizedAndForbiddenCodes() {
        assertEquals(401, Message.UNAUTHORIZED);
        assertEquals(403, Message.FORBIDDEN);
        assertEquals(0, Message.SUCCESS);
        assertEquals(2, Message.FAIL);
    }
}
```

Create `PageQueryTest.java`:

```java
package com.financial.cloud.entity;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {
    @Test
    void defaultPageSizeIsTwenty() {
        PageQuery q = new PageQuery();
        Page<Object> page = q.build();
        assertEquals(1, page.getCurrent());
        assertEquals(20, page.getSize());
    }

    @Test
    void clampsPageSizeToMax() {
        PageQuery q = new PageQuery();
        q.setPageSize(500);
        q.setPageNumber(2);
        Page<Object> page = q.build();
        assertEquals(2, page.getCurrent());
        assertEquals(100, page.getSize());
    }

    @Test
    void nonPositivePageSizeUsesDefault() {
        PageQuery q = new PageQuery();
        q.setPageSize(0);
        Page<Object> page = q.build();
        assertEquals(20, page.getSize());
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -Dtest=MessageAuthCodesTest,PageQueryTest test
```

Expected: compile/test failure because `UNAUTHORIZED` / `FORBIDDEN` missing and default size still `Integer.MAX_VALUE`.

- [ ] **Step 3: Implement Message constants**

In `Message.java`, after existing constants add:

```java
public static final int UNAUTHORIZED = 401;
public static final int FORBIDDEN = 403;
```

- [ ] **Step 4: Implement PageQuery caps**

Replace defaults and `build()`:

```java
public static final int DEFAULT_PAGE_NUM = 1;
public static final int DEFAULT_PAGE_SIZE = 20;
public static final int MAX_PAGE_SIZE = 100;

public <T> Page<T> build() {
    Integer pageNum = ObjectUtils.defaultIfNull(getPageNumber(), DEFAULT_PAGE_NUM);
    Integer pageSize = ObjectUtils.defaultIfNull(getPageSize(), DEFAULT_PAGE_SIZE);
    if (pageNum <= 0) {
        pageNum = DEFAULT_PAGE_NUM;
    }
    if (pageSize == null || pageSize <= 0) {
        pageSize = DEFAULT_PAGE_SIZE;
    } else if (pageSize > MAX_PAGE_SIZE) {
        pageSize = MAX_PAGE_SIZE;
    }
    Page<T> page = new Page<>(pageNum, pageSize);
    List<OrderItem> orderItems = buildOrderItem();
    if (CollUtil.isNotEmpty(orderItems)) {
        page.addOrder(orderItems);
    }
    return page;
}
```

- [ ] **Step 5: Run tests — expect PASS**

```powershell
.\mvnw.cmd -Dtest=MessageAuthCodesTest,PageQueryTest test
```

Expected: `BUILD SUCCESS`, both tests green.

- [ ] **Step 6: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add financial-cloud/src/main/java/com/financial/cloud/entity/Message.java financial-cloud/src/main/java/com/financial/cloud/entity/PageQuery.java financial-cloud/src/test/java/com/financial/cloud/entity/MessageAuthCodesTest.java financial-cloud/src/test/java/com/financial/cloud/entity/PageQueryTest.java
git commit -m "fix: cap PageQuery size and add Message 401/403 constants"
```

---

### Task 2: Default-deny interceptor + Message 401 response

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/autoconfigure/FinancialCloudMvcConfig.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/interceptor/PermissionInterceptor.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/UnauthorizedEntryPoint.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/authn/web/RefusedPoint.java`

**Interfaces:**
- Consumes: `Message.UNAUTHORIZED`, `Message.FORBIDDEN`
- Produces: All non-excluded paths require `SignedPrincipal`; unauthenticated calls get HTTP 401 + JSON `Message`

- [ ] **Step 1: Rewrite FinancialCloudMvcConfig intercept registration**

Replace the body of `addInterceptors` so it uses default-deny:

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    logger.debug("add PermissionInterceptor default-deny");
    permissionInterceptor.setMgmt(true);
    registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                    "/login/**",
                    "/captcha",
                    "/secretKey/**",
                    "/auth/token/refresh",
                    "/auth/entrypoint",
                    "/auth/refusedpoint",
                    "/open/func/list",
                    "/metadata/version",
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/exception/error/**"
            );
    logger.debug("PermissionInterceptor registered");
}
```

Remove the long historical `addPathPatterns` list.

- [ ] **Step 2: Change PermissionInterceptor to write Message JSON**

Replace the `principal == null` branch in `preHandle` (remove `RequestDispatcher` forward). Keep authenticate + principal check. Use:

```java
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;
import com.financial.cloud.entity.Message;
```

```java
if (principal == null) {
    logger.trace("No Authentication for URI {}", request.getRequestURI());
    writeUnauthorized(response);
    return false;
}
return true;
```

Add private method:

```java
private void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Message<Void> body = new Message<>(Message.UNAUTHORIZED, "Unauthorized");
    JsonMapper.builder().build().writeValue(response.getOutputStream(), body);
}
```

Remove unused `RequestDispatcher` import.

- [ ] **Step 3: Align UnauthorizedEntryPoint / RefusedPoint**

In `UnauthorizedEntryPoint.entryPoint`, replace the `HashMap` body with:

```java
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
Message<Void> body = new Message<>(Message.UNAUTHORIZED, "Unauthorized");
JsonMapper.builder().build().writeValue(response.getOutputStream(), body);
```

In `RefusedPoint.refusedPoint`:

```java
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setStatus(HttpServletResponse.SC_FORBIDDEN);
Message<Void> body = new Message<>(Message.FORBIDDEN, "Forbidden");
JsonMapper.builder().build().writeValue(response.getOutputStream(), body);
```

Add `import com.financial.cloud.entity.Message;` and remove unused `HashMap`/`Map` imports.

- [ ] **Step 4: Compile check**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add financial-cloud/src/main/java/com/financial/cloud/autoconfigure/FinancialCloudMvcConfig.java financial-cloud/src/main/java/com/financial/cloud/authn/web/interceptor/PermissionInterceptor.java financial-cloud/src/main/java/com/financial/cloud/authn/web/UnauthorizedEntryPoint.java financial-cloud/src/main/java/com/financial/cloud/authn/web/RefusedPoint.java
git commit -m "feat: default-deny auth interceptor with Message 401/403"
```

---

### Task 3: Business exception code ranges

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/enums/BookBusinessExceptionEnum.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/enums/OrgsBusinessExceptionEnum.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/enums/BusinessExceptionCodeRangesTest.java`

**Interfaces:**
- Consumes: none
- Produces: Book codes in `[510001, 519999]`; Orgs codes in `[520001, 529999]`; no overlap

- [ ] **Step 1: Grep for hard-coded old codes**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
rg "50000[0-9]" -g "*.java" -g "*.ts" -g "*.vue" -g "*.js"
```

If UI/tests hard-code specific `50000x` values, update those call sites in this task. Message-only UI needs no change.

- [ ] **Step 2: Write range test**

```java
package com.financial.cloud.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionCodeRangesTest {
    @Test
    void bookCodesAre51xxxx() {
        for (BookBusinessExceptionEnum e : BookBusinessExceptionEnum.values()) {
            assertTrue(e.getCode() >= 510001 && e.getCode() <= 519999, e.name());
        }
    }

    @Test
    void orgsCodesAre52xxxx() {
        for (OrgsBusinessExceptionEnum e : OrgsBusinessExceptionEnum.values()) {
            assertTrue(e.getCode() >= 520001 && e.getCode() <= 529999, e.name());
        }
    }

    @Test
    void bookAndOrgsDoNotOverlap() {
        for (BookBusinessExceptionEnum b : BookBusinessExceptionEnum.values()) {
            for (OrgsBusinessExceptionEnum o : OrgsBusinessExceptionEnum.values()) {
                assertNotEquals(b.getCode(), o.getCode(), b.name() + " vs " + o.name());
            }
        }
    }
}
```

- [ ] **Step 3: Run test — expect FAIL**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -Dtest=BusinessExceptionCodeRangesTest test
```

- [ ] **Step 4: Renumber enums (preserve relative order)**

`BookBusinessExceptionEnum`: map `500001→510001` … `500012→510012` (same offset `+10000` on each).

`OrgsBusinessExceptionEnum`: map `500001→520001` … `500011→520011` (offset `+20000`). Keep duplicate-code pairs that already shared a code (e.g. `SUB_USERS_EXISTS` / `SYNC_USERS_EXISTS`) still sharing the same new code.

- [ ] **Step 5: Run test — expect PASS**

```powershell
.\mvnw.cmd -Dtest=BusinessExceptionCodeRangesTest test
```

- [ ] **Step 6: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add financial-cloud/src/main/java/com/financial/cloud/enums/BookBusinessExceptionEnum.java financial-cloud/src/main/java/com/financial/cloud/enums/OrgsBusinessExceptionEnum.java financial-cloud/src/test/java/com/financial/cloud/enums/BusinessExceptionCodeRangesTest.java
git commit -m "refactor: split Book/Orgs business codes into 51/52 ranges"
```

---

### Task 4: `/api/v1` path rewrite Filter

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilter.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/autoconfigure/ApiV1PathRewriteAutoConfiguration.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilterTest.java`
- Modify: `financial-cloud/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (append new auto-config class if that file is how other autoconfigs are registered; otherwise rely on `@AutoConfiguration` component scan from `com.financial.cloud`)

**Interfaces:**
- Consumes: servlet request path after context-path
- Produces: For paths starting with `/api/v1/`, downstream sees path with prefix stripped; other paths unchanged

- [ ] **Step 1: Confirm how AutoConfiguration is registered**

Open `financial-cloud/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. If present and lists `com.financial.cloud.autoconfigure.*`, append:

```
com.financial.cloud.autoconfigure.ApiV1PathRewriteAutoConfiguration
```

Save **UTF-8 without BOM**.

- [ ] **Step 2: Write Filter implementation**

```java
package com.financial.cloud.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Maps /api/v1/** onto existing controller paths without changing @RequestMapping.
 */
public class ApiV1PathRewriteFilter extends OncePerRequestFilter {

    public static final String API_V1_PREFIX = "/api/v1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith(API_V1_PREFIX + "/") || path.equals(API_V1_PREFIX)) {
            String newPath = path.equals(API_V1_PREFIX) ? "/" : path.substring(API_V1_PREFIX.length());
            filterChain.doFilter(new RewrittenRequest(request, newPath), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static final class RewrittenRequest extends HttpServletRequestWrapper {
        private final String newPath;

        RewrittenRequest(HttpServletRequest request, String newPath) {
            super(request);
            this.newPath = newPath;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + newPath;
        }

        @Override
        public String getServletPath() {
            return newPath;
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
```

- [ ] **Step 3: Register Filter early (before MVC)**

```java
package com.financial.cloud.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.financial.cloud.web.filter.ApiV1PathRewriteFilter;

@AutoConfiguration
public class ApiV1PathRewriteAutoConfiguration {

    @Bean
    public FilterRegistrationBean<ApiV1PathRewriteFilter> apiV1PathRewriteFilter() {
        FilterRegistrationBean<ApiV1PathRewriteFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiV1PathRewriteFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        bean.setName("apiV1PathRewriteFilter");
        return bean;
    }
}
```

- [ ] **Step 4: Unit test with MockHttpServletRequest**

```java
package com.financial.cloud.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiV1PathRewriteFilterTest {

    @Test
    void stripsApiV1Prefix() throws Exception {
        ApiV1PathRewriteFilter filter = new ApiV1PathRewriteFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jinbooks-api/api/v1/book/fetch");
        request.setContextPath("/jinbooks-api");
        request.setServletPath("/api/v1/book/fetch");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final ServletRequest[] seen = new ServletRequest[1];
        FilterChain chain = (req, res) -> seen[0] = req;
        filter.doFilter(request, response, chain);
        assertInstanceOf(ApiV1PathRewriteFilter.RewrittenRequest.class, seen[0]);
        assertEquals("/jinbooks-api/book/fetch", ((jakarta.servlet.http.HttpServletRequest) seen[0]).getRequestURI());
        assertEquals("/book/fetch", ((jakarta.servlet.http.HttpServletRequest) seen[0]).getServletPath());
    }

    @Test
    void leavesNonV1PathsUntouched() throws Exception {
        ApiV1PathRewriteFilter filter = new ApiV1PathRewriteFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jinbooks-api/book/fetch");
        request.setContextPath("/jinbooks-api");
        request.setServletPath("/book/fetch");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final ServletRequest[] seen = new ServletRequest[1];
        FilterChain chain = (req, res) -> seen[0] = req;
        filter.doFilter(request, response, chain);
        assertSame(request, seen[0]);
    }
}
```

If `spring-boot-starter-test` / `spring-test` is already on the test classpath (Boot projects usually are), this compiles. If not, add test dependency in `financial-cloud/pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 5: Run unit tests**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -Dtest=ApiV1PathRewriteFilterTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add financial-cloud/src/main/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilter.java financial-cloud/src/main/java/com/financial/cloud/autoconfigure/ApiV1PathRewriteAutoConfiguration.java financial-cloud/src/test/java/com/financial/cloud/web/filter/ApiV1PathRewriteFilterTest.java financial-cloud/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat: rewrite /api/v1 requests onto existing controller paths"
```

---

### Task 5: Voucher batch load — remove N+1

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/persistence/service/impl/VoucherServiceImpl.java`

**Interfaces:**
- Consumes: existing `baseMapper.selectVoById`, `queryItems`, mappers
- Produces: private `Map<String, VoucherVo> queryByIds(Collection<String> ids)` used by `submitBatch`, `audit`, `delete`

- [ ] **Step 1: Add private batch helper and shared item enrichment**

1. Read the full private `queryItems(String voucherId)` method (items + auxiliary + any cash-flow enrichment).
2. Extract shared enrichment into something like:

```java
private void enrichItemVos(List<VoucherItemVo> itemVos, List<VoucherAuxiliary> auxiliaries) {
    // move the auxiliary-grouping (and cash-flow) logic from queryItems here unchanged
}
```

3. Change `queryItems` to: load items for one id → load aux for one id → `enrichItemVos` → return.

4. Add batch loader near `queryById`:

```java
private Map<String, VoucherVo> queryByIds(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
        return Map.of();
    }
    List<String> idList = ids.stream().filter(Objects::nonNull).distinct().toList();
    if (idList.isEmpty()) {
        return Map.of();
    }

    List<VoucherVo> vouchers = new ArrayList<>();
    for (String id : idList) {
        VoucherVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            vouchers.add(vo);
        }
    }
    // If VoucherMapper already has / can add selectVoByIds(IN (...)), use that instead of the loop.

    if (vouchers.isEmpty()) {
        return Map.of();
    }

    List<String> voucherIds = vouchers.stream().map(VoucherVo::getId).toList();
    List<String> userIds = vouchers.stream()
            .map(VoucherVo::getCreatedBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<String, String> userMap = new HashMap<>();
    if (!userIds.isEmpty()) {
        userInfoMapper.selectByIds(userIds).forEach(u -> userMap.put(u.getId(), u.getDisplayName()));
    }

    List<VoucherItemVo> allItems = voucherItemMapper.selectVoList(
            Wrappers.<VoucherItem>lambdaQuery().in(VoucherItem::getVoucherId, voucherIds));
    List<VoucherAuxiliary> allAux = voucherItemAuxiliaryMapper.selectList(
            Wrappers.<VoucherAuxiliary>lambdaQuery().in(VoucherAuxiliary::getVoucherId, voucherIds));

    Map<String, List<VoucherItemVo>> itemsByVoucher = allItems.stream()
            .collect(Collectors.groupingBy(VoucherItemVo::getVoucherId));
    Map<String, List<VoucherAuxiliary>> auxByVoucher = allAux.stream()
            .collect(Collectors.groupingBy(VoucherAuxiliary::getVoucherId));

    Map<String, VoucherVo> result = new LinkedHashMap<>();
    for (VoucherVo vo : vouchers) {
        vo.setCreatedName(userMap.get(vo.getCreatedBy()));
        List<VoucherItemVo> itemVos = new ArrayList<>(
                itemsByVoucher.getOrDefault(vo.getId(), List.of()));
        enrichItemVos(itemVos, auxByVoucher.getOrDefault(vo.getId(), List.of()));
        vo.setItems(itemVos);
        result.put(vo.getId(), vo);
    }
    return result;
}
```

Success criterion: items/aux/users loaded with O(1) queries per batch (plus at most one header query per id unless `selectVoByIds` exists) — never call `queryById` inside batch loops.
- [ ] **Step 2: Update submitBatch**

Replace the per-id `queryById` loop body start with:

```java
Map<String, VoucherVo> voucherMap = queryByIds(ids);
int count = 0;
for (String id : ids) {
    VoucherVo voucherVo = voucherMap.get(id);
    if (voucherVo == null) {
        return Message.failed("凭证不存在");
    }
    // ... rest unchanged: copy to VoucherChangeDto, skip empty items, call submit(dto, false)
}
```

Preserve original success/ignore messaging.

- [ ] **Step 3: Update audit**

```java
List<Voucher> vouchers = baseMapper.selectByIds(ids);
List<Voucher> auditVouchers = vouchers.stream()
        .filter(item -> VoucherStatusEnum.UNDER_REVIEW.getValue().equals(item.getStatus()))
        .toList();
Map<String, VoucherVo> voucherMap = queryByIds(
        auditVouchers.stream().map(Voucher::getId).toList());
for (Voucher auditVoucher : auditVouchers) {
    VoucherVo voucher = voucherMap.get(auditVoucher.getId());
    if (voucher == null) {
        continue;
    }
    // ... rest unchanged
}
```

- [ ] **Step 4: Update delete balance-restore loop**

Where `booksVouchers.forEach` calls `queryById`, replace with:

```java
Map<String, VoucherVo> voucherMap = queryByIds(
        booksVouchers.stream().map(Voucher::getId).toList());
for (Voucher t : booksVouchers) {
    VoucherVo booksVoucher = voucherMap.get(t.getId());
    if (booksVoucher == null) {
        continue;
    }
    // ... existing balance restore using booksVoucher
}
```

- [ ] **Step 5: Compile**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
cd C:\Users\Administrator\Projects\jinbooks
git add financial-cloud/src/main/java/com/financial/cloud/persistence/service/impl/VoucherServiceImpl.java
# include mapper/xml only if added
git commit -m "perf: batch-load vouchers to remove submit/audit/delete N+1"
```

---

### Task 6: Package + smoke verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: all prior tasks
- Produces: evidence that health, public routes, protected 401, and `/api/v1` rewrite work

- [ ] **Step 1: Full package**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
.\mvnw.cmd -DskipTests package
```

Expected: `BUILD SUCCESS`, jar at `target/financial-cloud-boot-*.jar`.

- [ ] **Step 2: Ensure MySQL is up** (Docker/Podman compose on host port **3307** as previously configured). If already running, skip.

- [ ] **Step 3: Start app**

```powershell
cd C:\Users\Administrator\Projects\jinbooks\jinbooks
java -jar target\financial-cloud-boot-1.1.0-ga.jar
```

Wait for log line containing `Started FinancialCloudApplication`.

- [ ] **Step 4: Smoke curls** (PowerShell examples)

```powershell
# health
Invoke-WebRequest -Uri http://localhost:2154/jinbooks-api/actuator/health -UseBasicParsing | Select-Object StatusCode, Content

# public login config (must NOT be 401)
Invoke-WebRequest -Uri http://localhost:2154/jinbooks-api/login/get -UseBasicParsing | Select-Object StatusCode

# protected without token (must be 401 + code 401)
try { Invoke-WebRequest -Uri http://localhost:2154/jinbooks-api/voucher/fetch -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__; $reader = [IO.StreamReader]::new($_.Exception.Response.GetResponseStream()); $reader.ReadToEnd() }

try { Invoke-WebRequest -Uri http://localhost:2154/jinbooks-api/book/fetch -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__; $reader = [IO.StreamReader]::new($_.Exception.Response.GetResponseStream()); $reader.ReadToEnd() }

# /api/v1 protected without token also 401
try { Invoke-WebRequest -Uri http://localhost:2154/jinbooks-api/api/v1/book/fetch -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
```

Expected:
- health → 200
- `/login/get` → 200 (or non-401 business response)
- `/voucher/fetch`, `/book/fetch`, `/api/v1/book/fetch` → HTTP 401 and JSON containing `"code":401` (or `401` without quotes depending on Jackson)

- [ ] **Step 5: Optional login smoke**

If you have a local admin password: sign in via `/login/signin`, then call `/book/fetch` and `/api/v1/book/fetch` with `Authorization: Bearer <token>`; both should return `code:0` lists.

- [ ] **Step 6: Stop the app** (Ctrl+C). No further commit required unless smoke forced small fixes — then commit those fixes with a clear message.

---

## Self-Review (plan vs spec)

| Spec requirement | Task |
|------------------|------|
| Default-deny + public whitelist | Task 2 |
| 401 Message, no forward | Task 2 |
| UnauthorizedEntryPoint / RefusedPoint Message | Task 2 |
| Message.UNAUTHORIZED / FORBIDDEN | Task 1 |
| Book 51xxxx / Orgs 52xxxx | Task 3 |
| `/api/v1` rewrite, old paths kept | Task 4 |
| PageQuery default 20 / max 100 | Task 1 |
| Voucher submitBatch/audit/delete N+1 | Task 5 |
| Package + smoke | Task 6 |
| Out of scope: Resource Server, frontend baseURL, index sweep | Not planned |

No TBD placeholders remain. Filter must register before interceptor sees rewritten paths so whitelist comparisons stay on post-rewrite servlet paths (e.g. `/login/**` still matches after strip).
