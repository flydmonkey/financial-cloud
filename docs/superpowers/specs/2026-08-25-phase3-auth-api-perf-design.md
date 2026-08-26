# 阶段 3 设计：认证加固 · API 规范化 · 性能收紧

**日期：** 2026-08-25  
**范围：** 单体模块 `jinbooks/`（Spring Boot 4.1 + JWT 拦截器 + Vue 前端 `financial-cloud-ui`）  
**状态：** 待用户审阅规格后进入实现计划

---

## 1. 背景与目标

阶段 0–2 已完成构建与依赖现代化。阶段 3 聚焦三类缺口：

| 支柱 | 现状问题 | 本波目标 |
|------|----------|----------|
| 认证 | `FinancialCloudMvcConfig` 白名单式 `addPathPatterns`，账套/凭证/日记账/报表等核心 API **未拦截** | 默认全部需登录；公开接口显式排除；401 统一 `Message` |
| API | 401/403 为裸 JSON；业务码 `Book*` / `Orgs*` 同用 `500xxx` 重叠；无版本前缀 | 错误体统一；码段收敛；提供 `/api/v1` 规范入口（旧路径兼容） |
| 性能 | `PageQuery.DEFAULT_PAGE_SIZE = Integer.MAX_VALUE`；凭证批处理循环 `queryById` | 分页上限；批量加载消除 N+1 |

**非目标（本波不做）：** OAuth2 Resource Server、前端全量改路径、全库索引专项、OpenAPI 重写。

---

## 2. 架构总览

```
客户端
  │  Authorization: Bearer <JWT>
  ▼
/jinbooks-api
  ├─ (可选) /api/v1/*  ──rewrite──►  控制器原路径 /*
  ├─ PermissionInterceptor（默认 /**，exclude 白名单）
  │     └─ 未登录 → 写 Message(code=401) + HTTP 401（不再 forward /auth/entrypoint）
  ├─ Controllers → Services
  └─ GlobalExceptionHandler → Message
```

认证仍基于现有 `AuthorizationUtils` + `AuthTokenService` + `SessionManager`；本波只改拦截策略与错误出口，不换令牌模型。

---

## 3. 认证加固

### 3.1 拦截策略

将 `FinancialCloudMvcConfig` 从「枚举需登录路径」改为「默认拦截全部 + 排除公开路径」：

```java
registry.addInterceptor(permissionInterceptor)
    .addPathPatterns("/**")
    .excludePathPatterns(PUBLIC_PATHS);
```

**公开路径（白名单，精确到实现时再核对注解）：**

| 路径 | 用途 |
|------|------|
| `/login/**` | 登录配置与签入（含 `/login/trust`） |
| `/captcha` | 图形验证码 |
| `/secretKey/**` | 登录加密公钥 |
| `/auth/token/refresh` | 刷新令牌（GET/POST） |
| `/auth/entrypoint` | 保留兼容（本波后可不再被 forward） |
| `/auth/refusedpoint` | 保留兼容 |
| `/open/func/list` | 开放功能列表（现有排除） |
| `/metadata/version` | 版本明文 |
| `/actuator/health`、`/actuator/info` | 健康检查（若启用） |
| `/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html` | SpringDoc（若生产关闭文档则可不放行） |
| `/exception/error/**` | 容器错误页映射 |

其余一律需有效登录主体（含 `/voucher/**`、`/book/**`、`/journal/**`、`/statement/**`、`/statistics/**`、`/salary/**`、`/employee/**`、`/standard*/**`、`/base/**`、`/settlement*/**`、`/filestorage/**`、`/logout` 等）。

### 3.2 未认证响应

修改 `PermissionInterceptor`：当 `principal == null` 时：

1. `response.setStatus(401)`
2. `Content-Type: application/json`
3. 写出与全局一致的 `Message`：`{ "code": 401, "message": "Unauthorized", "timestamp": ..., "data": null }`
4. **不再** `forward` 到 `/auth/entrypoint`

同步改造 `UnauthorizedEntryPoint` / `RefusedPoint`：若仍保留端点，响应体改为同一 `Message` 结构（403 用 `code: 403`），避免前端双解析路径。

前端 `financial-cloud-ui/src/utils/Request.ts` 已处理 `code === 401` 与 `response.status === 401`，无需为本波改协议；仅需回归登录跳转。

### 3.3 验收

- 未带 Token 访问 `/voucher/fetch`、`/book/fetch` → HTTP 401 + `Message.code=401`
- 带有效 Token → 与现网行为一致
- `/login/get`、`/captcha`、`/secretKey/get` 仍可匿名访问

---

## 4. API 规范化

### 4.1 统一错误体

继续使用 `com.financial.cloud.entity.Message` 作为唯一 API 包装：

| code | 含义 | 说明 |
|------|------|------|
| `0` | 成功 | 已有 `SUCCESS` |
| `2` | 一般失败 | 已有 `FAIL` |
| `400` | 参数/校验错误 | 与现有 Handler 对齐 |
| `401` | 未认证 | **新增常量** `Message.UNAUTHORIZED` |
| `403` | 无权限 | **新增常量** `Message.FORBIDDEN` |
| `405` | 方法不允许 | 已有用法 |
| `500` | 系统错误 | Handler 兜底 |
| `51xxxx` | 账套/科目业务 | 见下 |
| `52xxxx` | 组织机构业务 | 见下 |

`GlobalExceptionHandler` 保持 `Message` 出口；本波不改业务成功码 `0`。

### 4.2 业务码段收敛

当前 `BookBusinessExceptionEnum` 与 `OrgsBusinessExceptionEnum` 均使用 `500001+`，语义冲突。

| 枚举 | 新码段 | 迁移方式 |
|------|--------|----------|
| `BookBusinessExceptionEnum` | `510001` 起（保持相对顺序） | 改枚举常量值 |
| `OrgsBusinessExceptionEnum` | `520001` 起 | 改枚举常量值 |

前端若未硬编码这些码，仅展示 `message`，则无 UI 改动。实现时 grep `50000` 确认无硬编码依赖。

### 4.3 API 版本前缀

**策略：路径重写，控制器零改动双挂。**

- 增加 Filter（或 `HandlerInterceptor` 前置）：请求 URI（去掉 context-path `/jinbooks-api` 后）若以 `/api/v1/` 开头，则 strip 前缀后内部 forward/rewrite 到原路径。
  - 例：`/jinbooks-api/api/v1/voucher/fetch` → 处理为 `/voucher/fetch`
- 原路径继续可用（兼容期）；文档与后续新接口以 `/api/v1` 为规范入口。
- 鉴权在 rewrite 之后或基于最终 servlet path 生效（实现时保证拦截器看到的路径与白名单一致；推荐 rewrite 在 Filter 中改 `HttpServletRequest` wrapper 的 `servletPath`/`requestURI`）。

本波 **不** 批量修改控制器 `@RequestMapping`；**不** 强制改前端 baseURL（可选后续跟进）。

### 4.4 验收

- 业务异常仍返回 `Message`，Book/Orgs 码落在 `51`/`52` 段且不再重叠
- `/api/v1/book/fetch` 与 `/book/fetch` 行为一致（均需登录）
- 401/403 响应可被现有 `Request.ts` 识别

---

## 5. 性能收紧

### 5.1 分页上限

`PageQuery`：

- `DEFAULT_PAGE_SIZE`：由 `Integer.MAX_VALUE` 改为 **`20`**（与多数列表页默认一致）
- 增加 `MAX_PAGE_SIZE = 100`：`build()` 中若 `pageSize > MAX` 则钳制为 `MAX`；若 `pageSize <= 0` 则用默认
- 明确传 `pageSize` 的前端页面（多数已传 5/10）不受影响；未传参的接口从「全表」变为「默认 20 条」，属预期收紧

若个别管理端接口确需更大页：允许显式传 `pageSize` 最大 100；超出 100 的需求不在本波用「查全部」解决，应改为导出接口。

### 5.2 凭证批处理 N+1

`VoucherServiceImpl` 中至少以下路径存在循环 `queryById`：

- `submitBatch`
- `audit`
- 删除等批量路径（约 719 行附近）

**改造原则：**

1. 按 `ids` **一次**（或分批 IN 查询）加载凭证主表 + 明细，构建 `Map<id, VoucherVo>`
2. 循环内只做状态校验与业务提交/审核，不再逐条 `queryById`
3. 保持现有事务边界与「顺序提交」语义；失败时仍返回已成功条数提示

不引入新的缓存中间件。

### 5.3 验收

- 未传 `pageSize` 的分页 SQL `LIMIT` ≤ 100（默认 20）
- `submitBatch` / `audit` 在批量 N 条时，凭证查询次数为 O(1) 批，而非 O(N) 次 `queryById`

---

## 6. 实现顺序

1. **认证默认拦截 + 401 Message**（安全优先，可单独验证）
2. **错误码常量 + 401/403 端点对齐 + 业务码段重编号**
3. **`/api/v1` rewrite Filter**
4. **`PageQuery` 上限**
5. **凭证批量加载**
6. **打包启动 + 冒烟**（health、匿名白名单、受保护 API 401、登录后凭证列表、批提交）

---

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 遗漏公开接口导致登录页不可用 | 白名单对照 `LoginEndpoint`/`SecretKey`/`Captcha`/`AuthTokenRefresh`；启动后无 Token 打登录三件套 |
| 某接口依赖「未登录也能调」 | 冒烟清单覆盖；发现后加入白名单并记录原因 |
| 默认 pageSize 变小导致前端「少数据」 | 前端列表普遍已传 pageSize；对未传的接口属安全修复 |
| 业务码变更导致外部集成依赖旧码 | 仓库内 grep；本系统前端以 message 为主 |

---

## 8. 测试计划（实现后）

- [ ] `mvnw -DskipTests package` 成功
- [ ] 启动后 `/jinbooks-api/actuator/health` → 200
- [ ] 匿名：`/login/get`、`/captcha`、`/secretKey/get` → 非 401
- [ ] 匿名：`/voucher/fetch`、`/book/fetch`、`/journal/entry/fetch` → 401 + `Message`
- [ ] 登录后上述业务接口正常
- [ ] `/api/v1/book/fetch`（带 Token）与 `/book/fetch` 一致
- [ ] 分页未传 size 时不超过默认/上限
- [ ] 批量提交多张凭证无逐条 query 风暴（日志或断点确认）

---

## 9. 后续波次（本规格之外）

- Spring Security OAuth2 Resource Server 替换拦截器
- 前端 baseURL 切到 `/api/v1`
- 慢 SQL / 索引专项
- 权限模型（角色资源）与账套数据隔离强化
