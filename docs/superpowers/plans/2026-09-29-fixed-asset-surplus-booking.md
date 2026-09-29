# 固定资产盘盈入账 + 参数页修边 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completed inventory checks can book surplus (editable prorated amount → draft voucher + card update/split); polish voucher-settlement params page.

**Architecture:** Mirror `disposeDeficit`: `FixedAssetCheckService.surplusPreview` / `bookSurplus` orchestrate; draft voucher Dr 1601 / Cr 5301.04; qty=1 clones card via `FixedAssetCopyRules.nextCopyCode`. Params page is frontend-only.

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, Element Plus, JUnit 5 + Mockito.

## Global Constraints

- Pricing default: `originalValue / bookQuantity × (actualQuantity − bookQuantity)`; user may override; amount must be > 0.
- Card: bookQuantity==1 → new card; bookQuantity>1 → bump quantity and originalValue on same card.
- Scope: only existing `result=surplus` items; no off-book rows.
- Voucher: draft; Debit fixed-asset subject; Credit subject code chain `5301.04` then `5301`.
- Per-item try/catch like `disposeDeficit` (no class-level transaction wrapping the whole batch).
- Seal / open-period guards same as other fixed-asset writes.

---

## File map

| File | Responsibility |
|------|----------------|
| `sql/patches/2026-09-29-fixed-asset-surplus-booking.sql` | ALTER item columns |
| `sql/financial_cloud_init.sql` | Same columns on CREATE |
| `FixedAssetCheckItem.java` | `surplusAmount`, `surplusVoucherId`, `surplusAssetId` |
| `FixedAssetCheckDtos.java` | Preview/Book request & response DTOs |
| `FixedAssetCheckService.java` | `defaultSurplusAmount`, `surplusPreview`, `bookSurplus` |
| `FixedAssetService.java` | `createSurplusVoucher` (+ optional clone helper) |
| `FixedAssetCheckController.java` | GET/PUT endpoints |
| `FixedAssetCheckServiceTest.java` | Unit tests |
| `financial-cloud-ui/.../check.ts` + `check.vue` | Preview dialog + book button |
| `voucher-settlement.vue` | Route + dirty save + readonly alert |
| `docs/product/07-fixed-asset.md`, `20-gap-analysis.md`, backlog | As-built |

---

### Task 1: Schema + entity fields

**Files:**
- Create: `sql/patches/2026-09-29-fixed-asset-surplus-booking.sql`
- Modify: `sql/financial_cloud_init.sql` (`fixed_asset_check_item` CREATE)
- Modify: `financial-cloud/src/main/java/com/financial/cloud/domain/fixedasset/FixedAssetCheckItem.java`

- [ ] **Step 1: Write patch SQL**

```sql
ALTER TABLE `fixed_asset_check_item`
  ADD COLUMN `surplus_amount` decimal(18,2) DEFAULT NULL COMMENT '盘盈入账金额' AFTER `remark`,
  ADD COLUMN `surplus_voucher_id` varchar(45) DEFAULT NULL COMMENT '盘盈凭证ID' AFTER `surplus_amount`,
  ADD COLUMN `surplus_asset_id` varchar(45) DEFAULT NULL COMMENT '盘盈拆出新卡ID' AFTER `surplus_voucher_id`;
```

- [ ] **Step 2: Mirror columns in `financial_cloud_init.sql` CREATE for `fixed_asset_check_item`**

- [ ] **Step 3: Add fields on entity**

```java
private java.math.BigDecimal surplusAmount;
private String surplusVoucherId;
private String surplusAssetId;
```

- [ ] **Step 4: Commit**

```bash
git add sql/patches/2026-09-29-fixed-asset-surplus-booking.sql sql/financial_cloud_init.sql \
  financial-cloud/src/main/java/com/financial/cloud/domain/fixedasset/FixedAssetCheckItem.java
git commit -m "chore(fixed-asset): 盘盈入账明细字段（金额/凭证/新卡）"
```

---

### Task 2: DTOs + default amount helper (TDD)

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/dto/fixedasset/FixedAssetCheckDtos.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/fixedasset/FixedAssetCheckService.java`
- Modify: `financial-cloud/src/test/java/com/financial/cloud/service/fixedasset/FixedAssetCheckServiceTest.java`

**Interfaces:**
- Produces: `static BigDecimal defaultSurplusAmount(BigDecimal originalValue, Integer bookQty, Integer actualQty)`
- Produces DTOs: `SurplusPreviewVo`, `SurplusPreviewRow`, `SurplusBookItemDto`, `SurplusBookVo` (reuse `SkipReason`)

- [ ] **Step 1: Failing tests**

```java
@Test
void defaultSurplusAmount_proratesOriginalValue() {
    assertEquals(new BigDecimal("500.00"),
            FixedAssetCheckService.defaultSurplusAmount(new BigDecimal("1000"), 2, 3));
}

@Test
void defaultSurplusAmount_zeroWhenNoIncrease() {
    assertEquals(0, FixedAssetCheckService.defaultSurplusAmount(new BigDecimal("1000"), 1, 1).compareTo(BigDecimal.ZERO));
}
```

- [ ] **Step 2: Run — expect compile/fail**

```bash
cd financial-cloud
mvn -q "-Dtest=FixedAssetCheckServiceTest#defaultSurplusAmount_proratesOriginalValue" test
```

- [ ] **Step 3: Implement helper + DTOs**

```java
static BigDecimal defaultSurplusAmount(BigDecimal originalValue, Integer bookQty, Integer actualQty) {
    int book = bookQty != null ? bookQty : 0;
    int actual = actualQty != null ? actualQty : 0;
    int delta = actual - book;
    if (delta <= 0 || book <= 0 || originalValue == null
            || originalValue.compareTo(BigDecimal.ZERO) <= 0) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return originalValue
            .divide(BigDecimal.valueOf(book), 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(delta))
            .setScale(2, RoundingMode.HALF_UP);
}
```

DTO sketch:

```java
@Data
public static class SurplusPreviewRow {
    private String itemId;
    private String assetId;
    private String assetCode;
    private String assetName;
    private int bookQuantity;
    private int actualQuantity;
    private int surplusQuantity; // actual - book
    private BigDecimal defaultAmount;
    private String strategy; // "split_card" | "bump_qty"
}

@Data
public static class SurplusPreviewVo {
    private List<SurplusPreviewRow> rows = new ArrayList<>();
}

@Data
public static class SurplusBookItemDto {
    private String itemId;
    private BigDecimal amount;
}

@Data
public static class SurplusBookVo {
    private int processedCount;
    private List<SkipReason> skipped = new ArrayList<>();
}
```

- [ ] **Step 4: Run tests — PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(fixed-asset): 盘盈默认金额均摊与预览/入账 DTO"
```

---

### Task 3: createSurplusVoucher on FixedAssetService

**Files:**
- Modify: `FixedAssetService.java`
- Test: add focused tests in `FixedAssetCheckServiceTest` (mock voucher path) OR small `FixedAssetService` test if patterns exist

**Interfaces:**
- Produces: `String createSurplusVoucher(FixedAsset asset, BigDecimal amount, String summary)` — draft voucher; resolve FA subject from asset / `1601`; credit `5301.04`,`5301`; throw `BusinessException` if subjects missing.

- [ ] **Step 1: Implement by cloning structure of `createPurchaseVoucher`**, but:
  - Debit: FA subject, `amount`
  - Credit: surplus gain subject (`5301.04`, `5301`)
  - No tax line
  - Summary includes 盘盈 + check title

- [ ] **Step 2: Expose package/public method callable from `FixedAssetCheckService`**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(fixed-asset): 盘盈草稿凭证（借固定资产贷5301.04）"
```

---

### Task 4: surplusPreview + bookSurplus

**Files:**
- Modify: `FixedAssetCheckService.java`
- Modify: `FixedAssetCheckController.java`
- Modify: `FixedAssetCheckServiceTest.java`

**Interfaces:**
- Consumes: `defaultSurplusAmount`, `fixedAssetService.createSurplusVoucher`, asset mapper, `FixedAssetCopyRules.nextCopyCode`
- Produces: `SurplusPreviewVo surplusPreview(String checkId, String bookId)`
- Produces: `SurplusBookVo bookSurplus(String checkId, String bookId, List<SurplusBookItemDto> items)`

- [ ] **Step 1: Tests**

```java
@Test
void surplusPreview_listsUnbookedSurplusWithStrategy() { /* completed check, qty=1 → split_card */ }

@Test
void bookSurplus_skipsAlreadyBooked() { /* surplusVoucherId set → skipped */ }

@Test
void bookSurplus_rejectsNonPositiveAmount() { /* amount 0 → skipped or 400 per item */ }
```

- [ ] **Step 2: Implement `surplusPreview`**
  - Require completed check + `assertWritable`
  - Rows: `RESULT_SURPLUS` and blank `surplusVoucherId`
  - Load asset for `originalValue` / quantity; compute default amount; strategy from `bookQuantity == 1`

- [ ] **Step 3: Implement `bookSurplus`**
  - Map body by itemId; require amount > 0
  - Skip if already has voucher id / asset disposed
  - If bookQty==1: clone asset (copy fields, new id/code via `nextCopyCode`, quantity=surplusQty, originalValue=amount, accumDepreciation=0); insert; voucher on **new** card; set `surplusAssetId`
  - If bookQty>1: update asset quantity += delta, originalValue += amount; voucher on **same** card; `surplusAssetId` null
  - Persist item `surplusAmount`, `surplusVoucherId`
  - Catch → `skipped`

- [ ] **Step 4: Controller**

```java
@GetMapping("/surplus-preview/{id}")
public Message<SurplusPreviewVo> surplusPreview(...)

@PutMapping("/book-surplus/{id}")
public Message<SurplusBookVo> bookSurplus(@PathVariable String id,
    @RequestBody List<SurplusBookItemDto> items, @CurrentUser UserInfo user)
```

- [ ] **Step 5: Run**

```bash
mvn -q "-Dtest=FixedAssetCheckServiceTest" test
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(fixed-asset): 盘盈预览与入账 API（拆卡/加数量）"
```

---

### Task 5: Frontend check.vue + API

**Files:**
- Modify: `financial-cloud-ui/src/api/fixed-asset/check.ts`
- Modify: `financial-cloud-ui/src/views/fixed-asset/check.vue`

- [ ] **Step 1: API**

```ts
export function surplusPreviewFixedAssetCheck(id: string) {
  return request({ url: `/fixed-asset/check/surplus-preview/${id}`, method: 'get' })
}
export function bookSurplusFixedAssetCheck(id: string, data: { itemId: string; amount: number }[]) {
  return request({ url: `/fixed-asset/check/book-surplus/${id}`, method: 'put', data })
}
```

- [ ] **Step 2: UI**
  - Button「盘盈入账」next to「盘亏下账」when `status===completed` && `surplusCount>0`
  - Dialog table: code/name/surplusQty/defaultAmount editable/`strategy` label
  - Confirm → `bookSurplus`; alert summary like deficit; update deficit message to remove「需建卡后另行入账」or point to 盘盈入账

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(fixed-asset): 盘点页盘盈入账弹窗"
```

---

### Task 6: Params page polish (C)

**Files:**
- Modify: `financial-cloud-ui/src/views/config/voucher-settlement.vue`

- [ ] **Step 1: Changes**
  - Jump: `router.push('/settlement/settle-period')`
  - Keep `loadedVoucherReviewed` / `loadedArap` refs; `dirty` computed; disable Save when `!dirty`
  - Non-admin: `el-alert type="warning"` 「仅账套管理员可修改」

- [ ] **Step 2: Commit**

```bash
git commit -m "fix(ui): 凭证结账参数页跳转与脏保存"
```

---

### Task 7: Product docs + apply SQL note

**Files:**
- Modify: `docs/product/07-fixed-asset.md`, `docs/product/20-gap-analysis.md`, `docs/superpowers/plans/2026-09-29-autonomous-iteration-backlog.md`
- Spec status already 已批准; optionally mark 已实施 after code lands

- [ ] **Step 1: Mark 盘盈入账 as 已实现; backlog row 已落地**

- [ ] **Step 2: Commit**

```bash
git commit -m "docs: 盘盈入账 as-built 与差距清单"
```

---

## Spec coverage check

| Spec item | Task |
|-----------|------|
| Prorate + editable amount | 2, 4, 5 |
| qty=1 split / qty>1 bump | 4 |
| Existing surplus only | 4 |
| Dr FA / Cr 5301.04 draft | 3, 4 |
| Preview + book APIs | 4 |
| Item columns | 1 |
| check.vue dialog | 5 |
| Params polish | 6 |
| Docs | 7 |
| Skip already booked / disposed | 4 |

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-29-fixed-asset-surplus-booking.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  

**2. Inline Execution** — this session with executing-plans checkpoints  

Which approach?
