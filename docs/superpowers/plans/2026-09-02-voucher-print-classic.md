# 记账凭证打印（经典国标 / C5 续页）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `mode=print` 下的凭证打印改为经典国标风：C5 横向居中、每页 6 行、合计含人民币大写、备注进表、超过 6 行按同号续页。

**Architecture:** 抽出纯函数 `chunkVoucherPrintPages` 做 6 行分页与空行补齐；打印态用独立 DOM（不用 `el-table`）按页渲染样张结构；编辑态 UI 保持不变。样式以 `docs/voucher-print-preview-classic.html` 与规格为准。

**Tech Stack:** Vue 3 + TypeScript、现有 `voucher-edit.vue` / `convertToChinese`、`@page` CSS、Node 原生 test（`npx tsx --test`）测纯函数。

**Spec:** `docs/superpowers/specs/2026-09-02-voucher-print-classic-design.md`

## Global Constraints

- 纸张：C5 横向 `229mm × 162mm`；打印内容水平垂直居中
- 每页固定 6 行分录位；不足补空行
- 续页：同凭证号；标题「记账凭证（续）」；「第 n / 共 m 页」（仅 m>1）
- 合计、人民币（大写）、备注、签名：**仅末页**
- 行次：有效分录跨页连续编号；空行行次空白
- 不拆业务凭证号；不改编辑态大改；不引入服务端 PDF
- 打印过滤规则保持：仅保留有 `subjectCode` 的分录

---

## File Structure

| 文件 | 职责 |
|------|------|
| `financial-cloud-ui/src/utils/voucherPrint.ts` | 分页切片、空行补齐、行次编号（纯函数） |
| `financial-cloud-ui/src/utils/voucherPrint.test.ts` | 上述纯函数单测 |
| `financial-cloud-ui/src/views/voucher/voucher-edit.vue` | 打印态 DOM + C5/居中/分页样式；编辑态不动业务逻辑 |
| `docs/voucher-print-preview-classic.html` | 可选：与实现保持 C5/6 行一致（对照样张） |

---

### Task 1: 分页纯函数 + 单测

**Files:**
- Create: `financial-cloud-ui/src/utils/voucherPrint.ts`
- Create: `financial-cloud-ui/src/utils/voucherPrint.test.ts`

**Interfaces:**
- Produces:
  - `VOUCHER_PRINT_PAGE_SIZE = 6`
  - `VoucherPrintSourceItem`（至少含 `summary`, `subjectCode`, `detailedAccounts`, `debitAmount`, `creditAmount`, `auxiliary?`）
  - `VoucherPrintLine`: `{ summary, subjectLabel, auxLabel, debitAmount, creditAmount, lineNo: number \| null, isEmpty: boolean }`
  - `VoucherPrintPage`: `{ pageIndex: number, pageCount: number, isLast: boolean, isContinuation: boolean, rows: VoucherPrintLine[] }`
  - `filterPrintableItems(items): T[]` — 保留 truthy `subjectCode`
  - `buildSubjectLabel(item): string` — 优先 `detailedAccounts`，否则用 `subjectCode`
  - `buildAuxLabel(item): string` — 辅助项拼接；无则 `''`
  - `chunkVoucherPrintPages(items, pageSize = 6): VoucherPrintPage[]` — 先 filter，再分页；每页 `rows.length === pageSize`；空行 `isEmpty: true`, `lineNo: null`

- [ ] **Step 1: Write the failing test**

Create `financial-cloud-ui/src/utils/voucherPrint.test.ts`:

```ts
import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  VOUCHER_PRINT_PAGE_SIZE,
  filterPrintableItems,
  chunkVoucherPrintPages,
} from './voucherPrint.ts'

const line = (n: number) => ({
  summary: `s${n}`,
  subjectCode: `100${n}`,
  detailedAccounts: `${1000 + n} 科目${n}`,
  debitAmount: n,
  creditAmount: 0,
  auxiliary: [],
})

describe('filterPrintableItems', () => {
  it('keeps only rows with subjectCode', () => {
    const items = [line(1), { ...line(2), subjectCode: '' }, line(3)]
    assert.equal(filterPrintableItems(items).length, 2)
  })
})

describe('chunkVoucherPrintPages', () => {
  it('pads a short voucher to one page of 6 rows', () => {
    const pages = chunkVoucherPrintPages([line(1), line(2)])
    assert.equal(pages.length, 1)
    assert.equal(pages[0].rows.length, VOUCHER_PRINT_PAGE_SIZE)
    assert.equal(pages[0].isLast, true)
    assert.equal(pages[0].isContinuation, false)
    assert.equal(pages[0].rows[0].lineNo, 1)
    assert.equal(pages[0].rows[1].lineNo, 2)
    assert.equal(pages[0].rows[2].isEmpty, true)
    assert.equal(pages[0].rows[2].lineNo, null)
  })

  it('splits 7 lines into 2 pages; totals only marked on last', () => {
    const items = Array.from({ length: 7 }, (_, i) => line(i + 1))
    const pages = chunkVoucherPrintPages(items)
    assert.equal(pages.length, 2)
    assert.equal(pages[0].pageIndex, 1)
    assert.equal(pages[0].pageCount, 2)
    assert.equal(pages[0].isLast, false)
    assert.equal(pages[0].isContinuation, false)
    assert.equal(pages[1].isLast, true)
    assert.equal(pages[1].isContinuation, true)
    assert.equal(pages[0].rows.filter((r) => !r.isEmpty).length, 6)
    assert.equal(pages[1].rows.filter((r) => !r.isEmpty).length, 1)
    assert.equal(pages[1].rows[0].lineNo, 7)
  })

  it('returns one empty-padded page when no printable items', () => {
    const pages = chunkVoucherPrintPages([])
    assert.equal(pages.length, 1)
    assert.equal(pages[0].rows.every((r) => r.isEmpty), true)
    assert.equal(pages[0].isLast, true)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd financial-cloud-ui
npx --yes tsx --test src/utils/voucherPrint.test.ts
```

Expected: FAIL（模块不存在或导出缺失）

- [ ] **Step 3: Write minimal implementation**

Create `financial-cloud-ui/src/utils/voucherPrint.ts`:

```ts
export const VOUCHER_PRINT_PAGE_SIZE = 6

export type VoucherPrintSourceItem = {
  summary?: string
  subjectCode?: string
  detailedAccounts?: string
  debitAmount?: number | string | null
  creditAmount?: number | string | null
  auxiliary?: Array<{ name?: string; value?: string; label?: string } | string>
}

export type VoucherPrintLine = {
  summary: string
  subjectLabel: string
  auxLabel: string
  debitAmount: number | string | null
  creditAmount: number | string | null
  lineNo: number | null
  isEmpty: boolean
}

export type VoucherPrintPage = {
  pageIndex: number
  pageCount: number
  isLast: boolean
  isContinuation: boolean
  rows: VoucherPrintLine[]
}

export function filterPrintableItems<T extends { subjectCode?: string }>(items: T[]): T[] {
  return (items || []).filter((item) => !!item?.subjectCode)
}

export function buildSubjectLabel(item: VoucherPrintSourceItem): string {
  const detailed = (item.detailedAccounts || '').trim()
  if (detailed) return detailed
  return (item.subjectCode || '').trim()
}

export function buildAuxLabel(item: VoucherPrintSourceItem): string {
  const aux = item.auxiliary
  if (!aux || !Array.isArray(aux) || aux.length === 0) return ''
  const parts = aux
    .map((a) => {
      if (typeof a === 'string') return a
      return a.label || a.name || a.value || ''
    })
    .filter(Boolean)
  return parts.length ? `辅助：${parts.join('、')}` : ''
}

function emptyRow(): VoucherPrintLine {
  return {
    summary: '',
    subjectLabel: '',
    auxLabel: '',
    debitAmount: null,
    creditAmount: null,
    lineNo: null,
    isEmpty: true,
  }
}

function toLine(item: VoucherPrintSourceItem, lineNo: number): VoucherPrintLine {
  return {
    summary: item.summary || '',
    subjectLabel: buildSubjectLabel(item),
    auxLabel: buildAuxLabel(item),
    debitAmount: item.debitAmount ?? null,
    creditAmount: item.creditAmount ?? null,
    lineNo,
    isEmpty: false,
  }
}

export function chunkVoucherPrintPages(
  items: VoucherPrintSourceItem[],
  pageSize: number = VOUCHER_PRINT_PAGE_SIZE,
): VoucherPrintPage[] {
  const printable = filterPrintableItems(items)
  const pageCount = Math.max(1, Math.ceil(printable.length / pageSize))
  const pages: VoucherPrintPage[] = []

  for (let p = 0; p < pageCount; p++) {
    const slice = printable.slice(p * pageSize, (p + 1) * pageSize)
    const rows: VoucherPrintLine[] = slice.map((item, i) =>
      toLine(item, p * pageSize + i + 1),
    )
    while (rows.length < pageSize) rows.push(emptyRow())
    const pageIndex = p + 1
    pages.push({
      pageIndex,
      pageCount,
      isLast: pageIndex === pageCount,
      isContinuation: pageIndex > 1,
      rows,
    })
  }
  return pages
}
```

> 若项目里辅助字段结构不同，在 Task 2 接入时对照 `voucher-edit` 实际 `auxiliary` 形状微调 `buildAuxLabel`，并补一条单测；不要在 Task 1 臆造复杂结构。

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd financial-cloud-ui
npx --yes tsx --test src/utils/voucherPrint.test.ts
```

Expected: PASS（全部用例）

- [ ] **Step 5: Commit**

```bash
git add financial-cloud-ui/src/utils/voucherPrint.ts financial-cloud-ui/src/utils/voucherPrint.test.ts
git commit -m "$(cat <<'EOF'
feat: add voucher print page chunking helper

EOF
)"
```

---

### Task 2: 打印态 DOM（多页经典表）

**Files:**
- Modify: `financial-cloud-ui/src/views/voucher/voucher-edit.vue`（template + script；本任务先不管细 CSS，可用最小 class）

**Interfaces:**
- Consumes: `chunkVoucherPrintPages`, `VOUCHER_PRINT_PAGE_SIZE`, `VoucherPrintPage` from `@/utils/voucherPrint`
- Produces: 打印态可见多页结构；编辑态 DOM 路径不变

- [ ] **Step 1: Import helper and compute pages**

In `<script setup>` 增加：

```ts
import { chunkVoucherPrintPages, type VoucherPrintPage } from '@/utils/voucherPrint'

const printPages = computed<VoucherPrintPage[]>(() => {
  if (!isPrintMode.value) return []
  return chunkVoucherPrintPages(formData.value.items || [])
})

function formatPrintDate(dateStr: string | null | undefined): string {
  if (!dateStr) return ''
  const [y, m, d] = String(dateStr).split('-')
  if (!y || !m || !d) return String(dateStr)
  return `${y} 年 ${m} 月 ${d} 日`
}
```

合计金额继续用现有 `formData.debitAmount` / `creditAmount`（或现有 `createTableData` 写入值）；大写调用已有 `convertToChinese(Number(formData.debitAmount) || 0)`。

- [ ] **Step 2: Split template — edit vs print**

将现有包在 `#printable-content` 内的编辑/共用结构包进 `v-if="!isPrintMode"`。  
在其后增加 `v-else` 打印根节点（结构必须如下，字段名按表单实际对齐）：

```vue
<div
  v-else
  ref="printMe"
  id="printable-content"
  class="voucher-print-root"
>
  <div
    v-for="page in printPages"
    :key="page.pageIndex"
    class="voucher-print-sheet"
  >
    <div class="vp-title">{{ page.isContinuation ? '记账凭证（续）' : '记账凭证' }}</div>
    <div class="vp-date">{{ formatPrintDate(formData.voucherDate) }}</div>
    <div class="vp-meta">
      <div>核算单位：{{ formData.companyName }}</div>
      <div class="vp-meta-right">
        <span v-if="page.pageCount > 1">第 {{ page.pageIndex }} / 共 {{ page.pageCount }} 页</span>
        <span>{{ formatVoucherWordNum() }}</span>
        <span>附件 {{ formData.receiptNum || 0 }} 张</span>
      </div>
    </div>

    <table class="vp-table">
      <thead>
        <tr>
          <th class="col-summary">摘要</th>
          <th class="col-subject">会计科目</th>
          <th class="col-debit">借方金额</th>
          <th class="col-credit">贷方金额</th>
          <th class="col-no">行次</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in page.rows" :key="idx">
          <td class="summary">{{ row.summary }}</td>
          <td class="subject">
            {{ row.subjectLabel }}
            <span v-if="row.auxLabel" class="aux">{{ row.auxLabel }}</span>
          </td>
          <td class="col-debit">{{ row.isEmpty ? '' : formatAmount(row.debitAmount) }}</td>
          <td class="col-credit">{{ row.isEmpty ? '' : formatAmount(row.creditAmount) }}</td>
          <td class="col-no">{{ row.lineNo == null ? '' : row.lineNo }}</td>
        </tr>
      </tbody>
      <tfoot v-if="page.isLast">
        <tr>
          <td class="label">合　计</td>
          <td class="label-cn">
            <span class="cn-tag">人民币（大写）</span>
            <span class="cn-amount">{{ convertToChinese(Number(formData.debitAmount) || 0) }}</span>
          </td>
          <td class="col-debit">{{ formatAmount(formData.debitAmount) }}</td>
          <td class="col-credit">{{ formatAmount(formData.creditAmount) }}</td>
          <td></td>
        </tr>
        <tr class="remark-row">
          <td class="label">备　注</td>
          <td colspan="4" class="remark-value">{{ formData.remark }}</td>
        </tr>
      </tfoot>
    </table>

    <div v-if="page.isLast" class="vp-signs">
      <span>会计主管：<em>{{ formData.managerName }}</em></span>
      <span>过账：<em>{{ formData.senderName }}</em></span>
      <span>复核：<em>{{ formData.auditMemberName }}</em></span>
      <span>制单：<em>{{ formData.createdName }}</em></span>
    </div>
  </div>
</div>
```

注意：
- 编辑态仍保留原来的 `ref="printMe"` / `id="printable-content"` 在编辑容器上；打印态如上。
- `onPrint` / `printSpecificDiv` 流程不改；确保 print 模式 `onMounted` 里 `createTableData` 仍会跑，以便 `debitAmount`/`creditAmount` 有值（若 print 过滤 items 后未触发合计，在赋值 `formData` 后显式调用一次合计计算或在 `printPages` 旁用 `computed` 汇总借贷）。

合计兜底（若现有 `createTableData` 在 print 时序不可靠）：

```ts
const printDebitTotal = computed(() =>
  (formData.value.items || []).reduce(
    (s: number, it: any) => s + (Number(it.debitAmount) || 0),
    0,
  ),
)
const printCreditTotal = computed(() =>
  (formData.value.items || []).reduce(
    (s: number, it: any) => s + (Number(it.creditAmount) || 0),
    0,
  ),
)
```

表尾改用 `printDebitTotal` / `printCreditTotal`。

- [ ] **Step 3: Manual smoke（开发态）**

Run:

```bash
cd financial-cloud-ui
npm run typecheck
```

Expected: 与本改动相关无新增 TS 错误。

再用浏览器打开一张 ≤6 行凭证点打印，确认出现经典表结构（样式可仍粗糙）。

- [ ] **Step 4: Commit**

```bash
git add financial-cloud-ui/src/views/voucher/voucher-edit.vue
git commit -m "$(cat <<'EOF'
feat: render classic multi-page voucher print DOM

EOF
)"
```

---

### Task 3: C5 横向样式与居中

**Files:**
- Modify: `financial-cloud-ui/src/views/voucher/voucher-edit.vue`（`<style>` 增加打印样式；可用非 scoped 块或 `:deep`，避免被 scoped 吃掉 `@page`）
- Optional sync: `docs/voucher-print-preview-classic.html`（已是 C5 则跳过）

**Interfaces:**
- Consumes: Task 2 的 class 名（`voucher-print-root`, `voucher-print-sheet`, `vp-*`）
- Produces: C5 `@page`、页间分页、居中、国标视觉

- [ ] **Step 1: Add print stylesheet**

在 `voucher-edit.vue` 增加第二个 `<style lang="scss">`（**不要** `scoped`），写入核心规则（可按样张微调数值，但约束不变）：

```scss
@page {
  size: 229mm 162mm;
  margin: 10mm 12mm;
}

@media print {
  .voucher-print-root {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    margin: 0;
    padding: 0;
    background: #fff;
  }
  .voucher-print-sheet {
    width: 100%;
    max-width: 100%;
    box-shadow: none;
    border: none;
    page-break-after: always;
    break-after: page;
  }
  .voucher-print-sheet:last-child {
    page-break-after: auto;
    break-after: auto;
  }
  .top-funs,
  .contextmenu {
    display: none !important;
  }
}

.voucher-print-root {
  font-family: "Songti SC", "SimSun", "STSong", "Noto Serif CJK SC", serif;
  color: #1a1a1a;
}
.voucher-print-sheet {
  width: 210mm;
  margin: 0 auto 16px;
  padding: 14mm 16mm 12mm;
  background: #fffef9;
  border: 1px solid #c9c4b8;
}
.vp-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.55em;
  padding-right: 0.55em;
  border-bottom: 3px double #222;
  width: fit-content;
  min-width: 52%;
  margin: 0 auto 6px;
}
.vp-date {
  text-align: center;
  font-size: 14px;
  margin-bottom: 10px;
  letter-spacing: 0.08em;
}
.vp-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}
.vp-meta-right {
  display: flex;
  gap: 16px;
}
.vp-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 13px;
}
.vp-table th,
.vp-table td {
  border: 1px solid #222;
  padding: 7px 8px;
  vertical-align: middle;
}
.vp-table thead th {
  background: #f7f4ec;
  font-weight: 700;
  text-align: center;
}
.vp-table .col-summary { width: 22%; }
.vp-table .col-subject { width: 34%; }
.vp-table .col-debit,
.vp-table .col-credit {
  width: 18%;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.vp-table .col-no { width: 8%; text-align: center; }
.vp-table tbody tr td { height: 36px; }
.vp-table .aux {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #444;
}
.vp-table tfoot td {
  font-weight: 700;
  background: #faf8f2;
}
.vp-table tfoot .label {
  text-align: center;
  background: #f7f4ec;
  letter-spacing: 0.2em;
}
.vp-table tfoot .label-cn {
  text-align: left;
  padding-left: 10px;
}
.vp-table tfoot .remark-row td {
  font-weight: 400;
  background: #fffef9;
  height: 44px;
}
.vp-table tfoot .remark-value {
  text-align: left;
  font-weight: 400;
}
.vp-signs {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  padding: 0 8px;
  font-size: 13px;
}
.vp-signs em {
  font-style: normal;
  border-bottom: 1px solid #222;
  display: inline-block;
  min-width: 4.5em;
  margin-left: 2px;
  padding: 0 4px;
}
```

删除或覆盖旧的 `@media print and (orientation: …) { .app-container { width: 1090px } }`，避免把打印挤出 C5。

- [ ] **Step 2: Visual verify**

1. ≤6 行：单页，有大写合计与备注签名；打印预览选 **C5 横向**，内容居中。  
2. ≥7 行：≥2 页；首页无合计；末页有；同字号；页码显示。

- [ ] **Step 3: Commit**

```bash
git add financial-cloud-ui/src/views/voucher/voucher-edit.vue
git commit -m "$(cat <<'EOF'
style: apply C5 classic voucher print layout

EOF
)"
```

---

### Task 4: 辅助核算标签对齐 + 回归核对

**Files:**
- Modify: `financial-cloud-ui/src/utils/voucherPrint.ts`（按真实 auxiliary 结构）
- Modify: `financial-cloud-ui/src/utils/voucherPrint.test.ts`（若改了拼接规则）
- Modify: `financial-cloud-ui/src/views/voucher/voucher-edit.vue`（仅当编辑态误伤时修复）

**Interfaces:**
- Consumes: 编辑态 `item.auxiliary` 实际字段（打开 `voucher-edit.vue` 中辅助展示逻辑对照）
- Produces: 打印辅助行文案与编辑态可读信息一致

- [ ] **Step 1: Inspect auxiliary shape in voucher-edit**

在 `voucher-edit.vue` 中定位辅助展示（约 `auxiliary` / `SelectAuxiliary`），确认对象字段（如 `itemName`、`name`、`value`）。将 `buildAuxLabel` 改为与之一致，例如：

```ts
export function buildAuxLabel(item: VoucherPrintSourceItem): string {
  const aux = item.auxiliary
  if (!aux || !Array.isArray(aux) || aux.length === 0) return ''
  const parts = aux
    .map((a: any) => {
      if (typeof a === 'string') return a
      return a.itemName || a.name || a.label || a.value || ''
    })
    .filter(Boolean)
  return parts.length ? `辅助：${parts.join('、')}` : ''
}
```

- [ ] **Step 2: Re-run unit tests**

```bash
cd financial-cloud-ui
npx --yes tsx --test src/utils/voucherPrint.test.ts
```

Expected: PASS

- [ ] **Step 3: Edit-mode regression check**

确认非 print：添加分录、暂存、打印按钮仍打开临时页；编辑表格列未丢。

Run:

```bash
cd financial-cloud-ui
npm run typecheck
```

- [ ] **Step 4: Commit**

```bash
git add financial-cloud-ui/src/utils/voucherPrint.ts financial-cloud-ui/src/utils/voucherPrint.test.ts financial-cloud-ui/src/views/voucher/voucher-edit.vue
git commit -m "$(cat <<'EOF'
fix: align voucher print auxiliary labels with edit data

EOF
)"
```

---

## Spec coverage checklist

| 规格要求 | Task |
|----------|------|
| C5 横向 + 居中 | Task 3 |
| 每页 6 行、空行补齐 | Task 1–2 |
| 经典表头/表体列 | Task 2–3 |
| 合计 + 人民币大写 + 备注入表 | Task 2 |
| 签名仅末页 | Task 2 |
| 续页同号、（续）、页码 | Task 1–2 |
| 行次跨页连续、空行空白 | Task 1 |
| 过滤无 subjectCode | Task 1 |
| 0 条仍 1 页 | Task 1 |
| 不改编辑态业务 | Task 2（v-if 隔离） |
| 复用 convertToChinese | Task 2 |

## Self-review notes

- 无 TBD；函数名在 Task 1–4 一致。  
- 未引入 vitest 依赖，用 `tsx --test` 适配现有前端工程。  
- 打印合计用 `printDebitTotal`/`printCreditTotal`，避免 print 时序依赖 `createTableData`。
