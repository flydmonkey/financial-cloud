# 简化认证设计：Bearer JWT + 拦截器

日期：2026-08-25  
状态：已完成  
范围：`jinbooks` 单体后端认证栈瘦身（方案 A）

## 背景

当前 `com.jinbooks.authn` 叠加了 JWT、服务端 Session、Congress、SM2 密钥、CAS/Trusted、Spring Security 空壳等。实际鉴权已由 `PermissionInterceptor` 完成。目标是保留「能登录、能登出、能拦请求」，去掉多余复杂度。

## 已确认决策

| 项 | 选择 |
|----|------|
| 登录态 | Bearer JWT（前端约定基本不变） |
| 验证码 | 保留（`/login/get` + `/captcha` + `state`） |
| Refresh | 保留（`/auth/token/refresh`） |
| 实施方式 | 瘦身现有 authn，不推倒重写 |

## 目标

1. 登录：用户名 + 密码 + 验证码 → access token + refresh token  
2. 鉴权：拦截器校验 Bearer JWT  
3. 登出：作废当前 token（内存黑名单）  
4. 删除：CAS、Congress、SM2 SecretKey、Session 登录双轨、无用 Spring Security 装配

## 非目标

- 不改前端登录页主流程（API 路径与主要响应字段保持兼容）  
- 不做多端踢人、分布式会话  
- 本阶段不重新设计 SM2/RSA 密码传输（登录按明文密码接收）  
- 不在本阶段重做细粒度 RBAC（现有 authorities 字段可继续返回）

## 对外 API（兼容）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/login/get` | 机构、验证码策略、`state`（随机 JWT，不再下发 SM2 密钥） |
| GET | `/captcha` | 图形验证码（依赖 `state`） |
| POST | `/login/signin` | `username/password/captcha/state/authType` → `AuthJwt` |
| GET | `/auth/token/refresh` | `refresh_token` → 新 `AuthJwt` |
| GET | `/logout` | 作废当前 access（及关联 refresh） |

响应仍包含：`token`、`refresh_token`、`type`、`id`、`username`、`displayName`、`authorities` 等现有 `AuthJwt` 字段（可删无用字段，但不得破坏前端必读字段）。

## 鉴权模型

```
请求 → PermissionInterceptor
         → 取 Authorization: Bearer <access>
         → 验签 + 过期 + 黑名单
         → 解析用户声明写入请求上下文
         → @CurrentUser 可读
```

- Access JWT：HS512；claims 含 `sub`、`jti`、`userId`、`exp` 等  
- Refresh JWT：独立密钥/过期时间；绑定 `jti` 或 `userId`  
- **不再**要求服务端 Session 与 JWT `jti` 双轨同时有效  
- 登出：将 access `jti`（及可选 refresh `jti`）写入内存黑名单，TTL = 剩余有效期  
- 刷新：校验 refresh → 发新 access/refresh → 旧 refresh 进黑名单

## 组件取舍

### 保留并简化

- `LoginController` / `LogoutController` / captcha 端点  
- `AuthJwt` + `AuthJwtService` / `AuthTokenService` / `AuthRefreshTokenService`  
- `PermissionInterceptor` + Bearer 解析（去掉 congress cookie 旁路）  
- `@CurrentUser` 解析器  
- 用户名密码校验（Jdbc 用户加载 + PasswordEncoder；可压扁 provider/realm）  
- 验证码校验（mem cache + state JWT）  
- Caffeine 内存缓存（验证码、token 黑名单）

### 删除或停用

- `authn.support.cas`、`TrustedAuthenticationProvider`、`CasAuthnAutoConfiguration`  
- `CongressService` / `InMemoryCongressService` / 相关装配  
- `secretkey`（`SecretKeyProvider`、`SM2Utils`、`SecretKeyEndpoint`、`SecretKeyManager`）  
- 登录态 `SessionManager` 双写路径（登录不再 `createOnlineTicket` 到 SessionManager）  
- `SavedRequestAwareAuthenticationSuccessHandler`、`HttpSessionListenerAdapter`、`SessionSecurityContextHolderStrategy`（若无引用）  
- Spring Security `SecurityFilterChain` 中无用配置可缩到最小或移除（若无其它依赖）  
- IP 归属地登录旁路（若仅服务于复杂登录日志，可后置）

### 可选后置

- DB 在线会话列表 / 踢人管理页：本阶段可不写入；管理功能若依赖再单独补「JWT jti 登记表」  
- Session 超时调度：无 Session 双轨后可删或改为黑名单清理

## 登录流程（简化后）

1. `GET /login/get` → `state` + captcha 配置  
2. `GET /captcha?state=` → 图片；答案缓存  
3. `POST /login/signin` → 校验 state/captcha → 查用户 → 验密码 → 签发 JWT  
4. 后续请求带 Bearer access  
5. `GET /logout` → 黑名单作废  

## 风险与兼容

| 风险 | 处理 |
|------|------|
| 前端仍读 `secretKey`/`secretPublicKey` | `/login/get` 可返回空字段或省略；确认前端可忽略 |
| 前端用 SM2/RSA 加密密码 | 本阶段按明文；若加密导致登录失败，临时关掉前端加密或补回简单解密 |
| 原 Session 踢人/在线列表 | 本阶段弱化；管理页可能看不到实时会话 |
| 仅 JWT、无服务端态 | 登出靠黑名单；重启进程黑名单清空（可接受） |

## 验收标准

- [ ] 前端现有登录页可完成：取配置 → 验证码 → 登录 → 带 token 访问业务接口（Task 7：未执行 HTTP 冒烟，本机无 MySQL/Docker）  
- [ ] 未登录访问受保护接口返回 401（Task 7：未执行 HTTP 冒烟）  
- [ ] refresh 可换新 token（Task 7：未执行 HTTP 冒烟）  
- [ ] logout 后原 access 立即失效（Task 7：未执行 HTTP 冒烟）  
- [x] 主源码无 CAS/Congress/SecretKey/Session 双轨登录依赖（Task 7 grep 零匹配）  
- [x] `mvn -DskipTests compile` 通过（Task 7：compile + test-compile BUILD SUCCESS）  

## 实现顺序（概要）

1. 引入 token 黑名单服务；拦截器改为纯 JWT + 黑名单  
2. 登录签发不再依赖 SessionManager  
3. 登出写入黑名单  
4. 删除 CAS/Congress/SecretKey 及相关配置  
5. 压缩 provider/realm；清理 Spring Security 空壳  
6. 冒烟：login / captcha / API / refresh / logout  
