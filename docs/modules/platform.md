# 平台与基础设施

## 概述

平台层涵盖认证栈瘦身（Bearer JWT + 拦截器）、Java 包五层重构、遗留死代码清理，以及 API 规范化与性能优化计划。Spring Boot 应用名 `financial-cloud`，API 前缀 `/api`。

## 已实现功能

### 认证（authn 简化，已完成）

- Bearer JWT + `PermissionInterceptor`；Caffeine token 黑名单
- 保留：验证码、`/login/get` + `/captcha` + `state`、refresh、logout 作废 jti
- 已移除：CAS、Congress、SM2 SecretKey、Session 双轨登录

### 包结构（已落地）

```
com.financial.cloud/
├── domain.{功能}
├── dto.{功能}
├── controller.{功能}
├── repository.{功能}
├── service.{功能}
└── common
```

VO 并入 `dto`；MyBatis mapper 在 `repository/**/*.xml`。

### Spring Security

- 已移除 `spring-boot-starter-security`
- 保留 `spring-security-crypto`（PasswordEncoder）

## 配置

| 项 | 位置 |
|----|------|
| 数据源 | `application.yml` → `127.0.0.1:3307/financial_cloud` |
| 会话上限 | `financial-cloud.session.max-size` |
| 报表 strict | `financial-cloud.statement.*.strict-*` |

数据库初始化见 [sql/README.md](../../sql/README.md)。

## API 要点（认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/login/get` | 登录配置 + state |
| GET | `/captcha` | 图形验证码 |
| POST | `/login/signin` | 登录 → AuthJwt |
| GET | `/auth/token/refresh` | 刷新 token |
| GET | `/logout` | 黑名单作废 |

## 鉴权 Phase 3 进度

| 项 | 状态 |
|----|------|
| 默认拒绝拦截（`PermissionInterceptor` + `/**`） | **已落地**（见 `FinancialCloudMvcConfig`） |
| 401 / `Message.UNAUTHORIZED` | **已有**（`Message.UNAUTHORIZED = 401`） |
| `/api/v1` 路径 rewrite | **未实现** |

## 未实现 / 计划中

- **`/api/v1` 前缀 rewrite**（兼容期保留原路径）
- **Deadcode 换库**：自研 Redis → Spring Data Redis；验证码仍为 Hutool + Caffeine（非 kaptcha）
- **性能**：凭证批量 N+1 修复、慢 SQL 专项
- **分布式会话**、OAuth2 Resource Server（非目标）
