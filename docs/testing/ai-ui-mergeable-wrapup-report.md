# 可合并收尾短报告（post PR #19）

- **分支**：`cursor/mergeable-wrapup-e2e-60cb`
- **基线**：`origin/main` @ `1e76181`（PR #19：CI 固化 + 工资/报销产品口径）
- **环境**：FE `:3154` / BE `:2154` / MySQL `:3307`（本轮空库；无 `AI-UI-20260930` 夹具）
- **范围**：盯 main push 完整 e2e、评估 AI-UI CI 子集夹具、写清本轮结论。**未**重跑整本 AI UI 全流程。

## 1) main 完整 e2e（push job）

| 项 | 结果 |
|----|------|
| Run | [36816366004](https://github.com/flydmonkey/financial-cloud/actions/runs/36816366004) |
| headSha | `1e761810ceb7f1a9bcdc381acb6569da629fa986` |
| `backend-test` / `frontend-check` | **PASS** |
| `e2e`（push） | **PASS** |
| accounting / year-end / BS golden / IS golden | **PASS**（各 step conclusion=success） |
| AI UI CI subset（fixture-gated） | **SKIPPED**（notice：fixture books not present；exit 0） |

**根因回顾（上一轮红 → 本轮绿）**：合入账套权限硬校验后，审核员 `switchBook` 需 `permission_book`。PR #19 已在 `e2e/helpers/auth.ts` 补 `/api/book/members/grant`；本轮 main push 完整套件转绿，**本收尾无需再改 e2e 代码**。

## 2) AI-UI CI 子集

### 本轮实跑

| 命令 | 结果 |
|------|------|
| `AI_UI_REQUIRE_BOOKS=0 npm run test:ai-ui:ci-subset` | **SKIPPED**（本地空库 0 账套；与 CI 一致） |
| `AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset` | **FAIL exit 1**（门闸正确：无夹具则硬失败） |
| `EmployeeServicePayBaseTest` + `SalaryContributionBaseRulesTest` | **PASS**（`./mvnw test`） |
| OBS-PAY-BASE-FALLBACK 产品口径 | 单测覆盖：自定义基数缺失/非正 → `HrErrorCode.CUSTOM_PAY_BASE_REQUIRED`（**504005**）；`remaining-gaps` 已期望保存硬拒，不再 WARN 静默回退。**有夹具时才能在子集脚本里端到端确认** |

### 夹具 / 最小种子评估（未能把 CI SKIP 升为实跑）

| 方案 | 评估 | 结论 |
|------|------|------|
| 沿用 `AI-UI-20260930` 现有库 | 本环境 MySQL volume 为空；CI 亦是 `run_init_sql` 冷库 | **不可用** |
| 仅按名建空账套 | `ci-subset` 门闸会变绿，但子集脚本（尤其 `remaining-gaps`）硬编码 `BOOK_B`/`BOOK_C`/`EMP_ID`/`FA_CARD` 等雪花 ID，并要求既有工资员、固资卡、凭证状态 | **会从 SKIP 变成 FAIL**，更差 |
| 轻量 SQL fixture | 需导出整段 AI-UI 业务态（员工/固资/凭证/权限用户等），体量接近全流程产物，且 ID 与脚本耦合 | **本轮不落地**（超出「不重跑全流程」收口） |
| 脚本自举种子 | 等同重写/重跑 AI-UI 全流程提示词路径 | **明确不做** |

**阻塞**：PR/main 上空库子集只能保持 fixture-gated **SKIP**；本地保留夹具后应 `AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset` 实跑。升实跑需另开「夹具制品/workflow_dispatch 挂载 dump」专项，不在本 PR。

## 3) CI 怎么跑

```bash
# 始终
cd financial-cloud-ui && npm run test:ai-ui:scripts-check && npm run test:unit
cd financial-cloud && ./mvnw test -Dtest='EmployeeServicePayBaseTest,SalaryContributionBaseRulesTest'

# main push 完整 e2e：见 GHA job e2e（accounting → year-end → golden → fixture-gated AI-UI）

# 有 AI-UI-20260930 夹具时
cd financial-cloud-ui && AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset
```

## 4) 本收尾 PR CI

| 项 | 结果 |
|----|------|
| PR | [#20](https://github.com/flydmonkey/financial-cloud/pull/20) @ `3c6f20e` |
| Run | [36817151678](https://github.com/flydmonkey/financial-cloud/actions/runs/36817151678) |
| `backend-test` / `frontend-check` / `e2e-smoke` | **PASS** |
| 完整 `e2e` | PR 上 SKIP（仅 push→main；完整绿证见上文 §1） |

## 本轮支持范围（不宣称全系统全绿）

- ✅ main push 完整 Playwright e2e（含 accounting / year-end / golden）在 #19 合入后 **PASS**
- ✅ 本收尾 PR CI（含 e2e-smoke）**PASS**
- ✅ 工资自定义基数硬拒口径单测 **PASS**（504005）
- ✅ AI-UI 子集门闸行为正确（无夹具 SKIP / REQUIRE=1 失败）
- ❌ AI-UI 5 套件端到端实跑：**未宣称**（无夹具）
- ❌ 未重跑整本 AI UI 全流程
