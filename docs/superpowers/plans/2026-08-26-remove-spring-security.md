# 去除 Spring Security、拦截器鉴权 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or executing-plans to implement task-by-task.

**Goal:** 移除 `spring-boot-starter-security` 空壳，鉴权完全由 `PermissionInterceptor` + 自有认证模型承担，API 路径与前端兼容。

**Architecture:** 当前真实鉴权已在 `PermissionInterceptor` → `AuthorizationUtils` → `SessionManager`；Spring Security 仅 `permitAll` 过滤器链。先替换 `org.springframework.security.*` 类型为 `com.financial.cloud.authn` 自有类型，再删依赖与 Filter 注册。令牌模型本阶段**保持现有 Session/Bearer sessionId**（与前端兼容），JWT+黑名单作为可选第二波。

**Tech Stack:** Spring Boot 4.1、MVC Interceptor、Caffeine Session、`spring-security-crypto`（仅 PasswordEncoder）

## Global Constraints

- 不改前端登录主流程与 API 路径
- 不推送远端（用户要求）
- 本阶段不做前端按需/懒加载
- 保持 `/api/login/**`、`/api/captcha`、`/api/auth/token/refresh` 等行为兼容

---

## 现状摘要

| 组件 | 状态 |
|------|------|
| `PermissionInterceptor` | ✅ 已默认拒绝 + 白名单 |
| `AuthorizationUtils` | ✅ Bearer / cookie / param 解析 sessionId |
| `SecurityFilterChain` | ❌ 空壳，可删 |
| `spring-boot-starter-security` | ❌ 可删（密码用 crypto 子模块） |
| JWT 验签 + 黑名单 | ❌ 设计文档有，代码未落地 |

---

## Task 1: 自有认证类型

- [ ] 新增 `AuthAuthentication`（principal、credentials、authorities、authenticated）
- [ ] 新增 `Authority` / `SimpleAuthority` 替代 `GrantedAuthority`
- [ ] 新增 `AuthenticationException` / `BadCredentialsException`（authn 包内）
- [ ] `SignedPrincipal` 去掉 `UserDetails` 实现

## Task 2: 改造鉴权链路

- [ ] `AuthorizationUtils` 使用 `AuthAuthentication` 替代 `UsernamePasswordAuthenticationToken`
- [ ] `PermissionInterceptor` 去掉 spring-security import
- [ ] `CurrentUserMethodArgumentResolver` 适配新类型
- [ ] `AbstractAuthenticationProvider` / `NormalAuthenticationProvider` / `LoginController` 适配

## Task 3: 密码编码独立

- [ ] `pom.xml`：`starter-security` → `spring-security-crypto`
- [ ] `LegacyPasswordEncoders` / `ApplicationAutoConfiguration` 改用 crypto 包 import
- [ ] `JdbcAuthenticationRealm` 等验密逻辑不变

## Task 4: 删除 Spring Security 装配

- [ ] `FinancialCloudMvcConfig` 删除 `SecurityFilterChain` Bean
- [ ] `MvcAutoConfiguration` 删除 `SecurityContextHolderAwareRequestFilter` / `DelegatingFilterProxy`
- [ ] `SessionAutoConfiguration` 删除 `SavedRequestAwareAuthenticationSuccessHandler`
- [ ] 删除 `SavedRequestAwareAuthenticationSuccessHandler.java`、`HttpSessionListenerAdapter.java`（若无引用）

## Task 5: 测试与冒烟

- [ ] 更新 `AuthorizationUtilsTest`、`InMemorySessionManagerTest` 等
- [ ] `mvnw test` 通过
- [ ] 手工：login → 带 Bearer 访问 API → refresh → logout → 401

## Task 6（可选，第二波）: JWT + 黑名单

- [ ] 按 `docs/superpowers/specs/2026-08-25-authn-simplify-design.md` 实现 JWT 签发/验签
- [ ] `TokenBlacklistService`（Caffeine）
- [ ] 登录/登出/刷新改造；去掉 Session 双轨

---

## 关键文件

```
financial-cloud/pom.xml
configuration/FinancialCloudMvcConfig.java
configuration/MvcAutoConfiguration.java
authn/interceptor/PermissionInterceptor.java
authn/support/AuthorizationUtils.java
authn/SignedPrincipal.java
authn/provider/impl/NormalAuthenticationProvider.java
controller/auth/LoginController.java
controller/auth/LogoutController.java
```
