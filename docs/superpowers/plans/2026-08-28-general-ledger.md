# 总账 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增汇总型总账：区间折叠为每科目最多三行（期初/本期合计/本年累计），过滤 + 下钻明细账 + Excel 导出。

**Architecture:** 只读 `statement_subject_balance`，`StatementGeneralLedgerRules` 纯函数折叠/隐藏/展行；`StatementGeneralLedgerService` 查库+过滤级次；Controller 挂 `/api/statement/general-ledger`；前端 `general-ledger.vue` + span-method。

**Tech Stack:** Java 17 / Spring Boot / MyBatis-Plus / JUnit5；Vue3 / Element Plus；EasyExcel/POI（与费用明细一致用 POI 即可）

**Spec:** [docs/superpowers/specs/2026-08-28-general-ledger-design.md](../specs/2026-08-28-general-ledger-design.md)

## Global Constraints

- 不含凭证级流水、币别、未过账、打印
- 与科目余额表同源快照；跨期本期=各月本期之和；YTD=末日；期初=首月期初
- 勾稽容差 0.01
- 默认 `hideNoActivityAndZeroBalance=true`
- 期初行借/贷列留空；编码下钻 `/voucher/sub-ledger`
- 不主动 git commit（除非用户要求）

---

## File map

| 路径 | 职责 |
|------|------|
| `util/StatementGeneralLedgerRules.java` | 折叠、隐藏、展三行、期间码 |
| `dto/statement/StatementGeneralLedgerItem.java` | 行 DTO |
| `dto/statement/StatementGeneralLedgerReport.java` | 报表 DTO |
| `dto/statement/StatementParamsDto.java` | 增 from/to + 三个 hide 开关 |
| `service/statement/StatementGeneralLedgerService.java` | 查询/导出 |
| `controller/.../StatementReportController.java` | 端点 |
| `test/.../StatementGeneralLedgerRulesTest.java` | 规则单测 |
| `test/.../StatementGeneralLedgerServiceTest.java` | 服务单测（mock mapper） |
| `ui/.../statement-general-ledger.ts` | API |
| `ui/.../general-ledger.vue` | 页面 |
| `sql/seed/general_ledger_menu.sql` | 菜单 |

---

### Task 1: Rules — 折叠与展行

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/util/StatementGeneralLedgerRules.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/util/StatementGeneralLedgerRulesTest.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/StatementGeneralLedgerItem.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/StatementGeneralLedgerReport.java`

**Interfaces:**
- Produces:
  - `periodCode(String yearMonth)` → `yyyyMM`
  - `FoldedBalance` record/fields: openingDebit/Credit, periodDebit/Credit, ytdDebit/Credit, closingDebit/Credit, direction
  - `fold(List<StatementSubjectBalance> monthsOrdered)` — 首月期初、各月本期 sum、末日 YTD/期末
  - `shouldHideGroup(FoldedBalance, hideZeroBalance, hideNoActivityAndZeroBalance)`
  - `expandRows(code, name, periodCode, FoldedBalance, hidePeriodRowsWhenNoActivity)` → `List<Item>` with rowSpan

- [x] **Step 1: 写失败单测**（折叠跨期、隐藏、仅期初一行）
- [x] **Step 2: 跑测确认红**
- [x] **Step 3: 实现 Rules + DTO**
- [x] **Step 4: 跑测绿**

Run: `mvn -pl financial-cloud -Dtest=StatementGeneralLedgerRulesTest test`

---

### Task 2: Params + Service + Controller

**Files:**
- Modify: `StatementParamsDto.java` — `subjectCodeFrom`, `subjectCodeTo`, `hideZeroBalance`, `hideNoActivityAndZeroBalance`, `hidePeriodRowsWhenNoActivity`
- Create: `StatementGeneralLedgerService.java`
- Modify: `StatementReportController.java`
- Create: `StatementGeneralLedgerServiceTest.java`（mock `StatementSubjectBalanceMapper` + `BookSubjectMapper`）

**Interfaces:**
- Consumes: Rules + `StatementSubjectBalanceMapper.selectList` / 按 bookId+periods 查询
- Produces: `Message<StatementGeneralLedgerReport> query(StatementParamsDto)`；`export(...)`
- 默认：`hideNoActivityAndZeroBalance=true`；`periodType` 空则 `between`
- 过滤：code 闭区间、maxLevel、showAux（非辅助或 isAuxiliary）、三 hide
- 科目排序：按 subjectCode

- [x] **Step 1: Service 单测红**（mock 两月数据 → 三行；hide 默认）
- [x] **Step 2: 实现 Service + 端点 + DTO 字段**
- [x] **Step 3: 测绿**

Run: `mvn -pl financial-cloud -Dtest=StatementGeneralLedgerRulesTest,StatementGeneralLedgerServiceTest test`

---

### Task 3: 前端页面 + API + 菜单

**Files:**
- Create: `financial-cloud-ui/src/api/statement/statement-general-ledger.ts`
- Create: `financial-cloud-ui/src/views/statement/general-ledger.vue`
- Create: `sql/seed/general_ledger_menu.sql`

**Interfaces:**
- GET params 对齐 DTO；表格 span-method 用 `rowSpan`；编码 router 到 sub-ledger

- [x] **Step 1: API + vue（期间、过滤 popover、表、导出、下钻）**
- [x] **Step 2: 菜单 SQL（parent `1886357455563137026`，sort 紧挨费用明细）**

---

### Task 4: 验证

- [x] 后端单测全绿
- [x] 对照 spec 清单：折叠、隐藏、级次、下钻字段、导出列、无币别

---

## Self-review

- Spec 三行/折叠/过滤/API/菜单/下钻均有对应 Task
- 无 TBD；本期跨期用 Java sum（不误用 `groupCodeSubjectBalance` 只取末日本期）
