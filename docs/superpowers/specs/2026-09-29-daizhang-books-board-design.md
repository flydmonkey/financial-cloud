# 代账工作台 · 多账套月末看板

> Date: 2026-09-29  
> Status: **completed** — 2026-09-29；收口见 [books-board-backlog](../plans/2026-09-29-books-board-backlog.md)；PR [#6](https://github.com/flydmonkey/financial-cloud/pull/6)  
> Owner: product  
> Related: [吞吐 V2](2026-09-29-daizhang-throughput-v2-design.md)（已完成）、[Post-V2](2026-09-29-daizhang-post-v2-design.md)（已完成）、[21-roadmap](../../product/21-roadmap.md)、[00-overview](../../product/00-overview.md)

## 1. 背景与产品判断

「代账专业可用」与「吞吐 V2 / Post-V2」均已收口：单账套闭环（凭证→账簿→结账→三表→账本包）、打印/PDF、往来、日记账一致性与改证路径等已可用。

下一倍增点不在继续堆单账功能，而在：

> **代账会计同时服务 N 个账套时，缺少「一眼看清进度、跳进阻塞、批量交账」的跨账套工作台。**

竞品多账套看板证明该痛点真实；本仓库现状是顶栏切账套 + 单账首页 `TodoPanel`，无法回答「手上 30 套里哪几套还能结、哪几套该交账本包」。

### 1.1 主轴选择（已拍板）

| 选项 | 结论 |
|------|------|
| A 接手建账吞吐 | 不优先：期初 Excel / onboarding 已有，低频 |
| B 日常录账微创新 | 不优先：凭证工作台/导入/批量审过账已有 |
| **C 多账套月末工作台** | **采用**（见 §3 方案 1） |
| D 仅批量交付 ZIP | 过窄：不解决「哪套未结」 |

成功标准：做账员打开一个工作台即可 (1) 看到授权账套的账期与待办、(2) 一键进入阻塞处理、(3) 对已结账套批量导出账本包。

## 2. 目标用户

| 角色 | 能力 |
|------|------|
| 管理员 / 做账员 / 审核员 | 查看看板、进入处理、单套/批量导出账本包 |
| 查看员 | 只读列表；无导出、无「进入处理」写跳转 |

## 3. 方案选择（已拍板）

| 方案 | 内容 | 结论 |
|------|------|------|
| **1 轻量多账套看板** | 授权账套一览 + 待办/结账启发式 + 进入处理 + 批量账本包 | **采用** |
| 2 编排工作台 | 方案 1 + 跨账套完整 verify 编排 | 首期不做（性能与权限边界重） |
| 3 只做批量交付 | 无状态看板 | 不采用（缺诊断价值） |

## 4. 范围

### 4.1 做（本轮）

1. 侧栏新入口「代账工作台」，路由 `/workspace/books-board` → `views/workspace/books-board.vue`。
2. 授权账套一览表：名称、当前账期、待审、待过账、折旧待办、相对「关注月」的结账状态、账套状态（启用/禁用/封存）、阻塞摘要。
3. 行操作「进入处理」：`switchBook` 后按阻塞跳转（凭证列表 / 折旧 / 结账向导 / 结账列表）。
4. 单行与批量导出账本包（仅关注月已结）；批量由服务端打总 ZIP。
5. 筛选：关注月（默认自然月上月）、仅有待办、关键词搜账套名。

### 4.2 不做（Non-goals）

- **不做跨账套结账**（产品已确认）：不含跨账套一键/批量结账、批量过账；也不做「跨套结账就绪软报告」或跨套结账编排/就绪聚合。结账始终在单账套内完成；本工作台只做进度一览与进入单套处理
- 跨账套完整 `SettlementService.verify` 编排引擎
- 机构多租户 / 计费、移动端、AI、税局直连
- 替换现有单账首页看板与 `TodoPanel`（保留为当前账套视角）
- 重做月末五步向导或已交付的单套账本包主路径

## 5. 信息架构与交互

### 5.1 表格列

| 列 | 规则 |
|----|------|
| 账套名称 | `book.name` / `company_name` |
| 当前账期 | `configSysService.getCurrentTerm(bookId)` |
| 待审 | 与 `DashboardTodoService` 同口径；仅 `voucherReviewed=true` |
| 待过账 | 同现有口径 |
| 折旧 | `depreciationPending` →「待计提 / —」 |
| 结账状态 | 见 §5.2 启发式 |
| 账套状态 | 启用 / 禁用 / 封存 |
| 阻塞摘要 | 优先级：待审 → 待过账 → 待折旧 → 未结「可结账」→ 已结「可交账」；落后单独标红 |

### 5.2 结账启发式（写死）

关注月 `F`，账套当前开放账期 `C`：

| 条件 | 状态 |
|------|------|
| `C > F` | **已结**（关注月已过） |
| `C == F` | **未结** |
| `C < F` | **落后**（异常，标红，不参与批量导出） |

不以 `settlement` 表逐月反查为必选；与产品「结账后推进 `currentTerm`」语义一致。关注月默认 = 系统自然月的上月，页头可切换 `YYYY-MM`。

### 5.3 行操作「进入处理」

1. 调用现有 `switchBook(bookId)` 并刷新会话（与顶栏切换一致）。
2. 按阻塞摘要跳转：
   - 待审 / 待过账 → `/voucher/voucher-index`
   - 待折旧 → `/fixed-asset/depreciation`
   - 可结账 / **落后** → `/settlement/settle-period`
   - 可交账 → `/settlement/settle-list`
3. 封存账套：列表可见；禁用写路径「进入处理」；已结账期仍可导出。
4. 首页快捷入口含「代账工作台」→ `/workspace/books-board`。

### 5.4 批量导出

- 勾选「关注月已结」行（未结/落后勾选后跳过并提示）。
- 「批量导出账本包」→ 服务端总 ZIP，内含 `账套名/本月账本包.zip`；可选 `includeVoucherList`（默认与单套一致）。
- 部分失败：总 ZIP 仍含成功套；失败信息写入包内 `errors.txt`（或等价摘要）。

## 6. API 与组件边界

### 6.1 API

| 接口 | 说明 |
|------|------|
| `GET /api/workspace/books-board` | query：`focusPeriod`（可选）、`onlyTodo`、`keyword`；仅返回当前用户 `permission_book` 授权账套 |
| `GET /api/statement/books-pack/export` | **扩展**可选 `bookId`（须在授权内）；缺省仍用当前会话账套，兼容旧调用 |
| `POST /api/statement/books-pack/export-batch` | body：`bookIds[]`、`yearPeriod`、`includeVoucherList` → 总 ZIP |

账套数硬上限 **200**：超出部分不返回，响应头或 body 字段标明 `truncated=true` 与总授权数。首期同步聚合；>50 套的缓存/异步不在本轮。

### 6.2 后端单元

| 单元 | 职责 |
|------|------|
| `BooksBoardController` / `BooksBoardService` | 授权列表 + 行聚合 |
| `DashboardTodoService` | 保持单账 todo；抽取可复用计数逻辑供 Board，不把跨账套塞进 `/dashboard/todo` |
| `MonthlyBooksPackController` / `Service` | 单套 `bookId` 参数；batch 组装总 ZIP |

不改：`checkout` / `verify`、凭证写路径、首页经营卡片。

### 6.3 前端单元

| 单元 | 职责 |
|------|------|
| `views/workspace/books-board.vue` | 页头、筛选、表格、批量栏、空态 |
| `api/workspace/booksBoard.ts` | board + batch export |
| 菜单种子 `resources` | 「代账工作台」；角色可见性与仪表盘/结账同级（查看员只读） |

不改：`TodoPanel`、月末向导五步、账套管理 CRUD（工作台不替代账套管理）。

## 7. 权限

| 动作 | 规则 |
|------|------|
| 查看 board | 已登录；仅授权账套 |
| 进入处理 | 切账后走现有页面权限；封存禁用写跳转 |
| 单套/批量导出 | 与现有账本包同级；`bookId` 未授权 → 403；查看员前端隐藏导出，后端仍拒绝 |
| 跨账套写结账/过账 | **本轮不提供 API** |

## 8. 错误与空态

- 无授权账套：空态 + 引导 onboarding / 建账。
- 批量中单套失败：见 §5.4 `errors.txt`。
- 未授权 `bookId` 导出：403，不泄露其他账套是否存在的细节超出常规错误。

## 9. 测试要点

### 9.1 单测

- Board：多账套字段正确；无授权为空；封存/禁用状态正确。
- 启发式：`C>F` / `==` / `<` 三种。
- export `bookId`：授权通过 / 未授权 403 / 缺省当前账套。
- export-batch：2 套成功；1 成功 1 失败含 `errors.txt`。

### 9.2 E2E（最小）

- ≥2 授权账套 → 工作台多行。
- 未结行「进入处理」切换账套并到达目标页。
- 已结可导出；批量勾选已结可下载总 ZIP。

### 9.3 回归

- 首页 `TodoPanel` 仍只反映当前账套。
- 无 `bookId` 的旧账本包导出行为不变。

## 10. 文档与交付节奏

1. 本规格为权威设计；实施计划另文 `docs/superpowers/plans/`。
2. 合入后同步 `docs/product/00-overview.md`、`21-roadmap.md`（近端主轴指向本工作台）；可选 OpenSpec change `daizhang-books-board`。
3. 分支建议：`cursor/daizhang-books-board-*`；一刀可演示、可回滚。

## 11. 开放决策（本规格内已闭合）

| 题 | 决定 |
|----|------|
| 主战场 | 多账套月末工作台（轻量看板） |
| 结账判定 | `currentTerm` vs 关注月启发式，不强制扫 settlement 表 |
| 批量形态 | 服务端总 ZIP，非前端串行多文件 |
| 与 V2 关系 | V2/Post-V2 已完成；本规格为下一程序，不重开 V2 队列 |
