# 代账吞吐 V2 · W0+W1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Start the吞吐-V2 program: refresh product narrative + quality gate (W0), then ship subject-balance `showAux` filter with drill-to-sub-ledger (W1-1) and ARAP statement writeoff-status column (W1-2).

**Architecture:** Reuse existing `StatementParamsDto.showAux` and `statement_subject_balance.is_auxiliary` rows (already written on post). Extract a pure filter helper and apply it in `StatementReportService.subjectBalance`. For ARAP, extend detail/export with writeoff status derived from `ArapWriteoffService.loadWrittenOff` + `ArapWriteoffRules.remainingByItem`. No new tables.

**Tech Stack:** Spring Boot 4 + MyBatis-Plus, JUnit 5, Vue 3 + Element Plus, Playwright (optional minimal), Markdown product docs.

**Spec:** [docs/superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md](../specs/2026-09-29-daizhang-throughput-v2-design.md)

## Global Constraints

- Non-goals unchanged: 税局直连、工资条自助、移动端/AI、完整 SaaS 多租户、按钮级权限全铺开.
- One slice per commit narrative; no drive-by refactors.
- Every functional knife: related unit test (and/or reuse existing E2E); CI red ⇒ not done.
- Seal/open-period guards: do not weaken.
- `showAux` default remains **false** (hide auxiliary rows) — match current UI default.
- ARAP statement writeoff labels: `未核销` / `部分核销` / `已核销` only.
- W2–W4 are **out of this plan**; track in backlog, plan separately after W0+W1 merge.

---

## File map

| File | Responsibility |
|------|----------------|
| `docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md` | Live queue status for V2 |
| `docs/product/00-overview.md` | Point next phase to V2 |
| `docs/product/20-gap-analysis.md` | Program status → throughput V2 |
| `docs/product/21-roadmap.md` | Near-term axis = V2; remove stale L3/bank text |
| `docs/product/03-voucher.md` | Remove stale “无服务端 PDF” gap |
| `docs/product/04-ledger.md` | Document showAux + drill |
| `docs/product/10-system-admin.md` | Align backup/audit gaps with as-built |
| `docs/product/11-arap.md` | Statement writeoff column |
| `docs/quality-dashboard.md` | Refresh metrics date |
| `.../util/SubjectBalanceAuxFilter.java` | Pure showAux filter |
| `.../util/SubjectBalanceAuxFilterTest.java` | Unit tests |
| `.../service/statement/StatementReportService.java` | Call filter in `subjectBalance` |
| `.../views/statement/subject-balance.vue` | Pass showAux; drill link |
| `.../service/arap/ArapWriteoffRules.java` | `writeoffStatus(original, written)` |
| `.../dto/arap/ArapDetailLineVo.java` | `voucherItemId`, `writeoffStatus` |
| `.../service/arap/ArapService.java` | Fill status on detail + export column |
| `.../service/arap/ArapWriteoffRulesTest.java` | Status cases |
| `.../service/arap/ArapServiceTest.java` | Export/detail status if feasible |

---

### Task 1: W0 backlog tracker + product narrative refresh

**Files:**
- Create: `docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md`
- Modify: `docs/product/00-overview.md`
- Modify: `docs/product/20-gap-analysis.md`
- Modify: `docs/product/21-roadmap.md`
- Modify: `docs/product/03-voucher.md`
- Modify: `docs/product/10-system-admin.md`
- Modify: `docs/superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md` (status → approved)

**Interfaces:**
- Consumes: V2 design §5 queue IDs
- Produces: Live backlog file other tasks update

- [ ] **Step 1: Create live backlog**

Write `docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md`:

```markdown
# 吞吐 V2 自主迭代 backlog

> 规格：[2026-09-29-daizhang-throughput-v2-design.md](../specs/2026-09-29-daizhang-throughput-v2-design.md)
> 状态：进行中

## 回归门禁（W0-3）

每功能刀完成前必须：
1. 相关后端单测通过；
2. 复用既有相关 E2E **或** 新增 1 条最小回归；
3. CI 红则不得宣称完成。

## 队列

| ID | 切片 | 状态 |
|----|------|------|
| W0-1 | 产品叙事去陈 | 进行中 |
| W0-2 | 质量仪表盘刷新 | 待办 |
| W0-3 | 回归门禁约定 | 进行中（本文） |
| W1-1 | 科目余额 showAux + 穿透 | 待办 |
| W1-2 | 对账单核销状态 | 待办 |
| W2-1 | 红冲联动日记账 | 待办（另开计划） |
| W3-1 | 购入入账规则产品化 | 待办（另开计划） |
| W3-2 | 盘盈 bump 风险 | 待办（另开计划） |
| W4-1 | 逾期可配置硬阻断 | 待办（另开计划） |
| W4-2 | 程序收口 | 待办 |
```

- [ ] **Step 2: Update overview §7 “未实现/增强”**

Replace the Post-V1 bullet block so it points at throughput V2 (not “专业可用未完成”):

```markdown
### 下一阶段（专业可用已收口）

- 主轴：**代账吞吐第二曲线** — 见 [throughput V2 设计](../superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md) 与 [backlog](../superpowers/plans/2026-09-29-throughput-v2-backlog.md)
- Non-goal 不变：工资条员工自助、税局直连、移动端/AI
```

- [ ] **Step 3: Fix roadmap S2 stale lines**

In `docs/product/21-roadmap.md` §2 S2, replace the trailing “后续再做：核销 L3、银行调节…” paragraph with:

```markdown
**L3 核销、银行调节已落地。** 近端主轴改为 [吞吐 V2](../superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md)（辅助穿透、对账单核销状态、红冲×日记账等）。
```

Add at top of §2 (near-term):

```markdown
> **现行近端主轴（2026-09-29）**：吞吐 V2（W0–W4），不以 PRD 新模块扩容为主。
```

- [ ] **Step 4: Gap analysis program footer**

In `20-gap-analysis.md` §11 after “专业可用程序已收口”, append:

```markdown
**下一程序**：代账吞吐第二曲线 — [设计](../superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md) / [backlog](../superpowers/plans/2026-09-29-throughput-v2-backlog.md)。
```

- [ ] **Step 5: Scrub stale module gaps**

`03-voucher.md` §9: delete or strike the line claiming 服务端 PDF 未做（凭证仍为经典 HTML 打印；报表/账簿服务端 PDF 已有 — clarify accordingly).

`10-system-admin.md` §9: mark backup/audit items that are already delivered as done or remove obsolete “未实现” bullets; keep true gaps (系统级 dump、多租户升级).

- [ ] **Step 6: Spec status line**

Set design header Status to: `approved; implementation W0+W1 in progress`.

- [ ] **Step 7: Commit**

```bash
git add docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md \
  docs/superpowers/specs/2026-09-29-daizhang-throughput-v2-design.md \
  docs/product/00-overview.md docs/product/20-gap-analysis.md \
  docs/product/21-roadmap.md docs/product/03-voucher.md docs/product/10-system-admin.md
git commit -m "docs(product): W0 吞吐 V2 叙事去陈与 backlog 门禁"
```

---

### Task 2: W0-2 quality dashboard refresh

**Files:**
- Modify: `docs/quality-dashboard.md`
- Modify: `docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md` (mark W0-2)

- [ ] **Step 1: Run measurable commands** (best-effort; if env missing, record “未能本机跑通” + last known)

```bash
cd /workspace/financial-cloud-ui && npm run typecheck 2>&1 | tail -20
cd /workspace/financial-cloud-ui && npm run lint 2>&1 | tail -20
cd /workspace/financial-cloud && ./mvnw -q test 2>&1 | tail -40
```

- [ ] **Step 2: Rewrite dashboard header + totals** with today’s date and observed counts (TS error count from typecheck; lint error/warning; surefire summary). Keep “已知后续” but drop claims contradicted by as-built.

- [ ] **Step 3: Mark W0-2 done in backlog; commit**

```bash
git add docs/quality-dashboard.md docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md
git commit -m "docs(quality): refresh quality dashboard for throughput V2 W0"
```

---

### Task 3: W1-1 — `SubjectBalanceAuxFilter` (TDD)

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/util/SubjectBalanceAuxFilter.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/util/SubjectBalanceAuxFilterTest.java`

**Interfaces:**
- Produces: `List<StatementSubjectBalance> SubjectBalanceAuxFilter.apply(List<StatementSubjectBalance> rows, Boolean showAux)`
- Semantics: `showAux == null || showAux == false` ⇒ keep only rows where `isAuxiliary` is not `"y"`; `true` ⇒ return all rows (null-safe copy). Null/empty input ⇒ empty list.

- [ ] **Step 1: Write failing tests**

```java
package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.common.YesNoEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubjectBalanceAuxFilterTest {

    @Test
    void hideAuxWhenShowAuxFalseOrNull() {
        StatementSubjectBalance plain = row("1001", YesNoEnum.n.name());
        StatementSubjectBalance aux = row("1001_A1", YesNoEnum.y.name());
        assertEquals(1, SubjectBalanceAuxFilter.apply(List.of(plain, aux), false).size());
        assertEquals(1, SubjectBalanceAuxFilter.apply(List.of(plain, aux), null).size());
        assertEquals("1001", SubjectBalanceAuxFilter.apply(List.of(plain, aux), false).get(0).getSubjectCode());
    }

    @Test
    void keepAuxWhenShowAuxTrue() {
        StatementSubjectBalance plain = row("1001", YesNoEnum.n.name());
        StatementSubjectBalance aux = row("1001_A1", YesNoEnum.y.name());
        assertEquals(2, SubjectBalanceAuxFilter.apply(List.of(plain, aux), true).size());
    }

    @Test
    void nullSafeEmpty() {
        assertTrue(SubjectBalanceAuxFilter.apply(null, true).isEmpty());
    }

    private static StatementSubjectBalance row(String code, String isAux) {
        StatementSubjectBalance b = new StatementSubjectBalance();
        b.setSubjectCode(code);
        b.setIsAuxiliary(isAux);
        return b;
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (class missing)**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=SubjectBalanceAuxFilterTest test
```

Expected: compilation failure / class not found.

- [ ] **Step 3: Minimal implementation**

```java
package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.common.YesNoEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SubjectBalanceAuxFilter {
    private SubjectBalanceAuxFilter() {}

    public static List<StatementSubjectBalance> apply(List<StatementSubjectBalance> rows, Boolean showAux) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        if (Boolean.TRUE.equals(showAux)) {
            return new ArrayList<>(rows);
        }
        List<StatementSubjectBalance> out = new ArrayList<>();
        for (StatementSubjectBalance row : rows) {
            if (row == null) {
                continue;
            }
            if (!YesNoEnum.y.name().equals(row.getIsAuxiliary())) {
                out.add(row);
            }
        }
        return out;
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=SubjectBalanceAuxFilterTest test
```

- [ ] **Step 5: Commit**

```bash
git add financial-cloud/src/main/java/com/financial/cloud/util/SubjectBalanceAuxFilter.java \
  financial-cloud/src/test/java/com/financial/cloud/util/SubjectBalanceAuxFilterTest.java
git commit -m "feat(statement): SubjectBalanceAuxFilter for showAux"
```

---

### Task 4: W1-1 — wire filter into `subjectBalance` + UI drill

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/statement/StatementReportService.java` (`subjectBalance` ~441–496)
- Modify: `financial-cloud-ui/src/views/statement/subject-balance.vue`
- Modify: `docs/product/04-ledger.md`
- Modify: backlog W1-1 status

**Interfaces:**
- Consumes: `SubjectBalanceAuxFilter.apply`
- Produces: API respects `showAux`; UI navigates to `/voucher/sub-ledger?subjectCode=&date=`

- [ ] **Step 1: Apply filter after sort in `subjectBalance`**

Replace the commented block:

```java
//        if (!dto.getShowAux()) {
//            res = res.stream().filter(item -> item.getIsAuxiliary().equals(YesNoEnum.n.name())).toList();
//        }
        return new Message<>(res);
```

with:

```java
        res = SubjectBalanceAuxFilter.apply(res, dto.getShowAux());
        return new Message<>(res);
```

Add import for `SubjectBalanceAuxFilter`. Guard: if `res` was null before sort, initialize to empty list before sort/filter (avoid NPE).

- [ ] **Step 2: UI — subject code drill**

In `subject-balance.vue`, make the subject-code cell a link (mirror `general-ledger.vue` `goSubLedger`):

```ts
import { useRouter } from 'vue-router'
const router = useRouter()

function baseSubjectCode(code: string | undefined): string {
  if (!code) return ''
  const i = code.indexOf('_')
  return i > 0 ? code.slice(0, i) : code
}

function goSubLedger(row: any) {
  const code = baseSubjectCode(row.subjectCode)
  if (!code) return
  router.push({
    path: '/voucher/sub-ledger',
    query: { subjectCode: code, date: queryParams.value.reportDate },
  })
}
```

Template: wrap subject code display with `@click="goSubLedger(scope.row)"` + link styling.

Confirm `queryParams.showAux` is already sent via `selectGroupSubjectBalance(queryParams)` (it is on the object).

- [ ] **Step 3: Docs**

`04-ledger.md`: mark 科目余额「显示辅助核算」为已实现（开关生效）；凭证号/科目穿透：余额表科目编码可跳明细账。

- [ ] **Step 4: Verify**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=SubjectBalanceAuxFilterTest test
```

Manual/API smoke if stack up: `GET /api/statement/subject-balance?showAux=false` vs `true`.

- [ ] **Step 5: Commit**

```bash
git add financial-cloud/src/main/java/com/financial/cloud/service/statement/StatementReportService.java \
  financial-cloud-ui/src/views/statement/subject-balance.vue \
  docs/product/04-ledger.md docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md
git commit -m "feat(statement): honor showAux and drill subject-balance to sub-ledger"
```

---

### Task 5: W1-2 — writeoff status helper (TDD)

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/arap/ArapWriteoffRules.java`
- Modify: `financial-cloud/src/test/java/com/financial/cloud/service/arap/ArapWriteoffRulesTest.java`

**Interfaces:**
- Produces: `String ArapWriteoffRules.writeoffStatus(BigDecimal originalAbs, BigDecimal writtenOff)`  
  - written ≤ 0 or null → `未核销`  
  - written ≥ originalAbs (>0) → `已核销`  
  - else → `部分核销`  
  - originalAbs ≤ 0 → `未核销`

- [ ] **Step 1: Add failing tests** in `ArapWriteoffRulesTest`

```java
@Test
void writeoffStatusLabels() {
    assertEquals("未核销", ArapWriteoffRules.writeoffStatus(new BigDecimal("100"), BigDecimal.ZERO));
    assertEquals("部分核销", ArapWriteoffRules.writeoffStatus(new BigDecimal("100"), new BigDecimal("40")));
    assertEquals("已核销", ArapWriteoffRules.writeoffStatus(new BigDecimal("100"), new BigDecimal("100")));
    assertEquals("已核销", ArapWriteoffRules.writeoffStatus(new BigDecimal("100"), new BigDecimal("120")));
    assertEquals("未核销", ArapWriteoffRules.writeoffStatus(BigDecimal.ZERO, BigDecimal.ZERO));
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=ArapWriteoffRulesTest#writeoffStatusLabels test
```

- [ ] **Step 3: Implement**

```java
public static String writeoffStatus(BigDecimal originalAbs, BigDecimal writtenOff) {
    BigDecimal orig = originalAbs == null ? BigDecimal.ZERO : originalAbs;
    BigDecimal written = writtenOff == null ? BigDecimal.ZERO : writtenOff;
    if (orig.compareTo(BigDecimal.ZERO) <= 0 || written.compareTo(BigDecimal.ZERO) <= 0) {
        return "未核销";
    }
    if (written.compareTo(orig) >= 0) {
        return "已核销";
    }
    return "部分核销";
}
```

- [ ] **Step 4: Run — expect PASS; commit**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=ArapWriteoffRulesTest test
git add financial-cloud/src/main/java/com/financial/cloud/service/arap/ArapWriteoffRules.java \
  financial-cloud/src/test/java/com/financial/cloud/service/arap/ArapWriteoffRulesTest.java
git commit -m "feat(arap): writeoff status labels for statement lines"
```

---

### Task 6: W1-2 — detail + Excel statement column

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/dto/arap/ArapDetailLineVo.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/arap/ArapService.java`
- Modify: `docs/product/11-arap.md`
- Modify: backlog W1-2

**Interfaces:**
- Consumes: `loadWrittenOff`, `writeoffStatus`, movement `voucherItemId`
- Produces: detail lines with `writeoffStatus`; Excel column index 6 「核销状态」

- [ ] **Step 1: Extend DTO**

```java
private String voucherItemId;
private String writeoffStatus;
```

- [ ] **Step 2: In `detail(...)`, after building each line**

Before the loop, `Map<String, BigDecimal> written = arapWriteoffService.loadWrittenOff(bookId);`

When building `ArapDetailLineVo`:

```java
BigDecimal orig = ArapAgingCalculator.signedAmount(row, receivable).abs();
BigDecimal w = written.getOrDefault(row.getVoucherItemId() == null ? "" : row.getVoucherItemId(), BigDecimal.ZERO);
// ...
.voucherItemId(row.getVoucherItemId())
.writeoffStatus(ArapWriteoffRules.writeoffStatus(orig, w))
```

- [ ] **Step 3: In `exportStatement`, add header cell 6 「核销状态」 and per-row `line.getWriteoffStatus()`**

Shift nothing else; keep existing columns 0–5.

- [ ] **Step 4: Update `11-arap.md` 已知缺口** — remove “对账单 Excel 暂不强制标注核销状态”; note column exists.

- [ ] **Step 5: Run tests**

```bash
cd /workspace/financial-cloud && ./mvnw -Dtest=ArapWriteoffRulesTest,ArapServiceTest,ArapAgingCalculatorTest test
```

- [ ] **Step 6: Commit**

```bash
git add financial-cloud/src/main/java/com/financial/cloud/dto/arap/ArapDetailLineVo.java \
  financial-cloud/src/main/java/com/financial/cloud/service/arap/ArapService.java \
  docs/product/11-arap.md docs/superpowers/plans/2026-09-29-throughput-v2-backlog.md
git commit -m "feat(arap): statement export and detail show writeoff status"
```

---

### Task 7: W0+W1 closeout checkpoint

**Files:**
- Modify: backlog (W0/W1 statuses)
- Modify: design spec §5 checkmarks if desired (optional)

- [ ] **Step 1: Mark W0-1/2/3 and W1-1/1-2 completed in backlog**

- [ ] **Step 2: Push branch; update PR description with completed knives**

- [ ] **Step 3: Stop — next plan is W2-1 (红冲×日记账) only after this PR’s W0+W1 is green**

---

## Spec coverage self-check

| Spec item | Task |
|-----------|------|
| W0-1 叙事去陈 | Task 1 |
| W0-2 质量仪表盘 | Task 2 |
| W0-3 回归门禁约定 | Task 1 backlog section |
| W1-1 科目余额辅助穿透 | Task 3–4 |
| W1-2 对账单核销状态 | Task 5–6 |
| W2–W4 | Explicitly deferred |
| Non-goals | Global Constraints |

## Follow-on plans (not this file)

1. `2026-09-29-throughput-v2-w2-journal-reverse.md` — W2-1  
2. `2026-09-29-throughput-v2-w3-fixed-asset.md` — W3-1/W3-2  
3. `2026-09-29-throughput-v2-w4-settlement-closeout.md` — W4-1/W4-2  
