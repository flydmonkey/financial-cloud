# 质量加固短报告（CI + 产品口径收口）

- **分支**：`cursor/quality-hardening-ci-obs-584b`
- **基线**：`origin/main` @ `a8c4618`（含 AI UI 全流程合入与 post-merge 复测）
- **环境**：FE `:3154` / BE `:2154` / MySQL `:3307`（本轮本地可复测项以单测为主；AI-UI 夹具账套未随空库恢复）

## 本轮改了什么

### 1) CI 固化

| 项 | 说明 |
|----|------|
| `financial-cloud-ui/scripts-ai-ui-ci-subset.mjs` | 精简子集：section6 + remaining-gaps + final-gaps + guards + export-check |
| `npm run test:ai-ui:ci-subset` | 本地/有夹具环境跑子集 |
| `npm run test:ai-ui:scripts-check` | `node --check` 脚本语法；挂在 `frontend-check` |
| PR `e2e-smoke` job | 拉起 MySQL/BE/FE 后跑 `test:e2e:smoke`（opening-balance），并探测 AI-UI 子集 |
| push `e2e` job | 仍跑完整套件；末尾同样 fixture-gated 跑 AI-UI 子集 |

**e2e 在 PR 上曾为 SKIPPED 的原因**：`.github/workflows/ci.yml` 中 `e2e` 使用 `if: github.event_name == 'push'`，PR 事件不进入该 job（省时）。本轮在 PR 上新增 `e2e-smoke` 最小冒烟。

**完整 e2e 当前阻塞（main push 仍可能红）**：合入账套权限硬校验后，审核员 `switchBook` 需 `permission_book`。本轮已在 `e2e/helpers/auth.ts` 为审核员补 `/api/book/members/grant`；若仍有失败，需另开缺陷跟完整 accounting 套件，不在本轮宣称全绿。

**AI-UI 子集在空库 CI 的阻塞**：依赖已有 `AI-UI-20260930` 账套，不是冷启动种子。CI 使用 `AI_UI_REQUIRE_BOOKS=0`：无夹具时写 `docs/testing/ai-ui-ci-subset-report.md` 并 SKIP（exit 0）；有夹具则实跑 5 套件。本地保留夹具时：`AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset`。

### 2) 产品口径

| OBS | 处理 | 复测 |
|-----|------|------|
| OBS-PAY-BASE-FALLBACK | 自定义基数未填正数：员工保存即拒（`504005`）；`payBaseNumber` 支持写 null；UI 提示不清空回退 | `EmployeeServicePayBaseTest` + remaining-gaps 期望硬拒 |
| OBS-PAY-TEMPLATE-SI-TAX | **保持** SMB `jt_gz`/`zf_gz` 两行口径；产品说明与工资凭证规则页写死「不含个税/社保分录」 | 文档 / 模板页文案 |
| OBS-EXP-SUMMARY-LEN | 报销保存：字段 maxlength、超长截断，并提示「摘要已截断至 64 字」 | `voucherSummary.test.ts` |

### 3) CI 怎么跑

```bash
# 始终（PR/main frontend-check）
cd financial-cloud-ui && npm run test:ai-ui:scripts-check && npm run test:unit

# 后端相关单测
cd financial-cloud && ./mvnw test -Dtest='EmployeeServicePayBaseTest,SalaryContributionBaseRulesTest'

# 有 AI-UI-20260930 夹具时
cd financial-cloud-ui && AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset

# PR 冒烟（GHA e2e-smoke）
cd financial-cloud-ui && npm run test:e2e:smoke
```

## 复测结果（本轮支持范围）

| 项 | 结果 |
|----|------|
| `EmployeeServicePayBaseTest`（4）+ `SalaryContributionBaseRulesTest`（4） | **PASS** |
| `npm run test:unit`（含 `voucherSummary`，31 tests） | **PASS** |
| `npm run test:ai-ui:scripts-check` | **PASS** |
| AI-UI 5 套件全量 | **未跑通宣称**：本环境 MySQL 为空库，无 `AI-UI-20260930` 夹具；CI 为 fixture-gated SKIP/有夹具再跑 |
| 完整 Playwright accounting | **未宣称通过**：仅修审核员授权并开 PR 冒烟；完整套件仍走 push job |

> 不宣称「全系统全部通过」。本轮支持：CI 接线、三条产品口径收口、e2e SKIPPED 原因说明与 PR 最小冒烟、审核员账套授权修复。
