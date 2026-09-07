# 凭证导入 UX 增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish Excel import/export workbook formatting, add overwrite/skip/cancel on word-number conflicts for unposted vouchers, and expose batch「提交审核」for selected drafts in the list「更多」menu.

**Architecture:** Extend existing `POST /voucher/import` with optional `conflictMode`; when omitted and unposted conflicts exist, return `needsConflictDecision` without writes. Style workbook via shared POI helpers in `VoucherService`. Frontend conflict dialog re-posts with mode. Batch submit reuses `submitBatch` from「更多」.

**Tech Stack:** Java 17 / Spring Boot / Apache POI / Vue 3 + Element Plus / existing voucher APIs

## Global Constraints

- Column contract unchanged: 凭证日期、凭证字头、凭证字号、附单据数、备注、摘要、科目编码、借方金额、贷方金额
- Overwrite only unposted (`senderId` blank); posted → fail that group
- After overwrite → status `draft`
- Empty word number → auto-assign (no conflict UI)
- Reject legacy 凭证字/账套ID sheets
- No new submit API — use `POST /voucher/submit/{ids}`
- Do not commit unless user asks

---

## File map

| File | Responsibility |
|------|----------------|
| `financial-cloud/.../dto/voucher/VoucherImportResultVo.java` | Add `needsConflictDecision`, `skipped`, `conflicts` |
| `financial-cloud/.../service/voucher/VoucherService.java` | Workbook styling; conflict detect; overwrite/skip import |
| `financial-cloud/.../controller/voucher/VoucherController.java` | Pass `conflictMode` into import |
| `financial-cloud/.../VoucherImportExcelTest.java` | Unit tests for conflict helpers / styling headers |
| `financial-cloud-ui/src/api/voucher/voucher.ts` | `importVouchers(formData)` already; append conflictMode field |
| `financial-cloud-ui/src/views/voucher/voucher-index.vue` | Conflict dialog; 提交审核 menu; keep result dialog |

---

### Task 1: Extend import result VO

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/dto/voucher/VoucherImportResultVo.java`
- Test: compile only (field presence)

**Interfaces:**
- Produces: `needsConflictDecision: boolean`, `skipped: int`, `conflicts: List<ConflictItem>` where `ConflictItem` has `row`, `wordHead`, `wordNum`, `wordLabel`, `existingStatus`, `existingId`, `posted: boolean`

- [ ] **Step 1: Add fields to VO**

```java
private boolean needsConflictDecision;
private int skipped;
private List<ConflictItem> conflicts = new ArrayList<>();

@Data
public static class ConflictItem implements Serializable {
    private int row;
    private String wordHead;
    private Integer wordNum;
    private String wordLabel;
    private String existingStatus;
    private String existingId;
    private boolean posted;
}
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl financial-cloud -DskipTests compile` from repo root (or `cd financial-cloud; mvn -q -DskipTests compile`)  
Expected: BUILD SUCCESS

---

### Task 2: Workbook styling (template + export)

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/voucher/VoucherService.java` (`downloadImportTemplate`, `export`)
- Test: `financial-cloud/src/test/java/com/financial/cloud/service/voucher/VoucherImportExcelTest.java`

**Interfaces:**
- Produces: private helpers `styleHeaderRow`, `applyColumnWidths`, `writeInstructionSheet`, fixed widths array matching `VOUCHER_IO_HEADERS`

- [ ] **Step 1: Add failing test for instruction sheet name constant**

```java
@Test
void workbookSheets_useInstructionAndVoucherNames() {
    assertEquals("填写说明", VoucherService.SHEET_INSTRUCTIONS);
    assertEquals("凭证", VoucherService.SHEET_DATA);
}
```

- [ ] **Step 2: Run test — expect FAIL (constants missing)**

Run: `mvn -q "-Dtest=VoucherImportExcelTest#workbookSheets_useInstructionAndVoucherNames" test`  
Expected: FAIL compilation or assertion

- [ ] **Step 3: Implement styling in VoucherService**

Add:

```java
static final String SHEET_INSTRUCTIONS = "填写说明";
static final String SHEET_DATA = "凭证";
static final int[] COLUMN_WIDTHS = {14, 10, 10, 10, 20, 28, 14, 14, 14}; // characters * 256 later
```

In `downloadImportTemplate`:
1. Create sheet `填写说明` with bullet lines (column meanings, continuation rows, date format, subject code, conflict UX).
2. Create sheet `凭证` with styled header (bold + light gray fill), freeze pane row 1, sample rows with light yellow fill, amount cells numeric format `0.00`, set column widths from `COLUMN_WIDTHS`.
3. Do **not** rely only on `autoSizeColumn`.

In `export`:
1. Same header style / freeze / widths / amount format on data sheet `凭证` (no instruction sheet required).
2. Keep one-row-per-line flatten logic.

- [ ] **Step 4: Re-run test — PASS**

- [ ] **Step 5: Manual smoke after later restart** — download template opens with 2 sheets

---

### Task 3: Conflict detection + import modes (backend)

**Files:**
- Modify: `VoucherService.importFromExcel`, `saveImportGroup`
- Modify: `VoucherController.importExcel` to accept `conflictMode` request param or form field
- Test: `VoucherImportExcelTest`

**Interfaces:**
- Consumes: `VoucherImportResultVo` conflict fields
- Produces: `importFromExcel(String bookId, ExcelImport file, String conflictMode)`
  - `conflictMode` null/blank → if any **unposted** conflict, set `needsConflictDecision=true`, fill `conflicts`, return SUCCESS message「存在字号冲突，请选择处理方式」, **no DB writes**
  - `overwrite` → unposted conflict → `update` as draft; posted conflict → fail group; non-conflict → `save` draft
  - `skip` → unposted conflict → increment `skipped`; posted → fail; else save

Conflict key: `bookId + wordHead + voucherYear + voucherMonth + wordNum` where year/month from `voucherDate`.

Lookup:

```java
Voucher existing = baseMapper.selectOne(Wrappers.<Voucher>lambdaQuery()
    .eq(Voucher::getBookId, bookId)
    .eq(Voucher::getWordHead, wordHead)
    .eq(Voucher::getVoucherYear, year)
    .eq(Voucher::getVoucherMonth, month)
    .eq(Voucher::getWordNum, wordNum)
    .last("LIMIT 1"));
boolean posted = existing != null && StringUtils.isNotBlank(existing.getSenderId());
boolean unpostedConflict = existing != null && !posted;
```

Overwrite path: build same `VoucherChangeDto` as create, set `id=existing.getId()`, `status=draft`, clear sender/audit/manager fields, call `update(dto)` (existing draft/update validation). Do **not** auto-bump word number on overwrite.

When `conflictMode` blank and only posted conflicts (no unposted): proceed with writes; posted groups fail with message「字号已过账，不可覆盖」.

- [ ] **Step 1: Write unit tests**

```java
@Test
void classifyConflict_unpostedVsPosted() {
    // pure helper if extracted:
    // assertFalse(isPosted(voucherWithNullSender));
    // assertTrue(isPosted(voucherWithSender));
}
```

Prefer extracting:

```java
static boolean isPosted(Voucher v) {
    return v != null && StringUtils.isNotBlank(v.getSenderId());
}
```

Test that method.

- [ ] **Step 2: Implement `importFromExcel(..., String conflictMode)` and controller wiring**

Controller:

```java
@PostMapping("/import")
public Message<VoucherImportResultVo> importExcel(
    @ModelAttribute("excelImportFile") ExcelImport excelImportFile,
    @RequestParam(value = "conflictMode", required = false) String conflictMode,
    @CurrentUser UserInfo userInfo) {
    ProductRoles.requireWriteVoucher();
    return voucherService.importFromExcel(userInfo.getBookId(), excelImportFile, conflictMode);
}
```

Also accept `conflictMode` from multipart form field with same name (Spring binds both).

- [ ] **Step 3: Run `VoucherImportExcelTest` + `VoucherServiceTest`**

Run: `mvn -q "-Dtest=VoucherImportExcelTest,VoucherServiceTest" test`  
Expected: PASS

---

### Task 4: Frontend conflict dialog + import retry

**Files:**
- Modify: `financial-cloud-ui/src/api/voucher/voucher.ts`
- Modify: `financial-cloud-ui/src/views/voucher/voucher-index.vue`

**Interfaces:**
- Consumes: `res.data.needsConflictDecision`, `res.data.conflicts`
- Produces: FormData with `excelFile` + optional `conflictMode`

- [ ] **Step 1: Keep file reference for retry**

In `handleImportUpload`, store `item.file` in `pendingImportFile` ref when needs decision.

- [ ] **Step 2: Conflict dialog UI**

`el-dialog` title「字号冲突」:
- table of conflicts (字号、状态、行号)
- buttons: 覆盖 / 跳过冲突 / 取消

```ts
function retryImport(mode: 'overwrite' | 'skip') {
  const formData = new FormData()
  formData.append('excelFile', pendingImportFile.value)
  formData.append('conflictMode', mode)
  voucherApis.importVouchers(formData).then(...)
}
```

- [ ] **Step 3: Handle first response**

```ts
if (res.code === 0 && res.data?.needsConflictDecision) {
  conflictDialog.conflicts = res.data.conflicts || []
  conflictDialog.visible = true
  return
}
// else existing result dialog / success toast
```

Cancel: clear pending file, close dialog, no second request.

- [ ] **Step 4: Manual verify** — import file with existing unposted word → dialog → overwrite/skip/cancel

---

### Task 5: 「更多 → 提交审核」

**Files:**
- Modify: `financial-cloud-ui/src/views/voucher/voucher-index.vue` only

**Interfaces:**
- Consumes: existing `handleSubmit` / `submitBatch` / `filterVoucherIdsByStatus('draft')`

- [ ] **Step 1: Add menu item**

Under「更多」, before 删除:

```vue
<el-dropdown-item
  :disabled="ids.length === 0"
  @click="handleSubmit()"
>
  提交审核
</el-dropdown-item>
```

Ensure `handleSubmit()` without row uses selection (`filterVoucherIdsByStatus('draft')` already supports that pattern — verify call signature matches audit handlers).

- [ ] **Step 2: Manual verify** — select drafts → 提交审核 → status reviewing or completed per book setting

---

### Task 6: Rebuild backend + smoke

**Files:** none (ops)

- [ ] **Step 1: Package and restart port 2154** (same pattern as prior sessions)

```powershell
mvn -q -DskipTests package
# stop listener on 2154, start jar
```

- [ ] **Step 2: Smoke checklist from spec §验收要点**

1. 下载模板：两 Sheet、表头样式  
2. 冲突三选一行为  
3. 已过账字号失败可读  
4. 提交审核入口  

---

## Spec coverage check

| Spec requirement | Task |
|------------------|------|
| Excel 说明 Sheet + 样式 | Task 2 |
| 冲突预检 + overwrite/skip/cancel | Task 3–4 |
| 覆盖未过账 → draft | Task 3 |
| 已过账不可覆盖 | Task 3 |
| 更多 → 提交审核 | Task 5 |
| 复用 submitBatch | Task 5 |

## Placeholder scan

None intentional.
