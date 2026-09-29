/**
 * Classic voucher print HTML — CSS/markup aligned with
 * docs/voucher-print-preview-classic.html (no visual drift).
 */
import {
  chunkVoucherPrintPages,
  type VoucherPrintPage,
  type VoucherPrintSourceItem,
} from './voucherPrint.ts'

/** Exact stylesheet from docs/voucher-print-preview-classic.html (+ multi-page break). */
export const CLASSIC_VOUCHER_PRINT_CSS = `
@page { size: 229mm 162mm; margin: 10mm 12mm; }
* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  padding: 28px 24px 36px;
  background: #e8e6e1;
  font-family: "Songti SC", "SimSun", "STSong", "Noto Serif CJK SC", serif;
  color: #1a1a1a;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.sheet {
  width: 210mm;
  min-height: 148mm;
  margin: 0 auto 16px;
  padding: 14mm 16mm 12mm;
  background: #fffef9;
  box-shadow: 0 8px 28px rgba(0,0,0,.12);
  border: 1px solid #c9c4b8;
}
.title {
  text-align: center;
  font-size: 28px;
  letter-spacing: 0.55em;
  padding-right: 0.55em;
  font-weight: 700;
  line-height: 1.2;
  padding-bottom: 6px;
  border-bottom: 3px double #222;
  margin: 0 auto 6px;
  width: fit-content;
  min-width: 52%;
}
.meta-date {
  text-align: center;
  font-size: 14px;
  margin-bottom: 10px;
  letter-spacing: 0.08em;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  margin-bottom: 8px;
}
.meta-row .right { display: flex; gap: 22px; }
table.voucher {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 13px;
}
table.voucher th,
table.voucher td {
  border: 1px solid #222;
  padding: 7px 8px;
  vertical-align: middle;
}
table.voucher thead th {
  background: #f7f4ec;
  font-weight: 700;
  text-align: center;
}
table.voucher .col-summary { width: 22%; }
table.voucher .col-subject { width: 34%; }
table.voucher .col-debit,
table.voucher .col-credit { width: 18%; text-align: right; font-variant-numeric: tabular-nums; }
table.voucher .col-no { width: 8%; text-align: center; }
table.voucher tbody td.subject,
table.voucher tbody td.summary { text-align: left; }
table.voucher tbody tr td { height: 36px; }
table.voucher .aux {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #444;
}
table.voucher tfoot td {
  font-weight: 700;
  background: #faf8f2;
}
table.voucher tfoot .label {
  text-align: center;
  background: #f7f4ec;
  font-weight: 700;
  letter-spacing: 0.2em;
}
table.voucher tfoot .label-cn {
  text-align: left;
  padding-left: 10px;
  font-weight: 700;
}
table.voucher tfoot .cn-tag {
  display: inline-block;
  margin-right: 10px;
  letter-spacing: 0.05em;
}
table.voucher tfoot .cn-amount { letter-spacing: 0.12em; }
table.voucher tfoot tr.remark-row td {
  font-weight: 400;
  background: #fffef9;
  height: 44px;
}
table.voucher tfoot tr.remark-row .label {
  font-weight: 700;
  background: #f7f4ec;
  letter-spacing: 0.2em;
}
table.voucher tfoot .remark-value {
  text-align: left;
  padding: 8px 10px;
  line-height: 1.45;
  font-weight: 400;
}
.signs {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  padding: 0 8px;
  font-size: 13px;
}
.signs span { min-width: 18%; }
.signs em {
  font-style: normal;
  border-bottom: 1px solid #222;
  display: inline-block;
  min-width: 4.5em;
  margin-left: 2px;
  padding: 0 4px;
}
@media print {
  html, body {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
    background: #fff;
  }
  body {
    display: flex !important;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
  }
  .sheet {
    box-shadow: none;
    border: none;
    background: #fff;
    width: 100%;
    max-width: 100%;
    min-height: auto;
    padding: 2mm 4mm;
    margin: 0 auto;
    page-break-after: always;
    break-after: page;
  }
  .sheet:last-child {
    page-break-after: auto;
    break-after: auto;
  }
}
`.trim()

export type ClassicVoucherPrintInput = {
  companyName?: string | null
  voucherDate?: string | null
  wordHead?: string | null
  wordNum?: number | string | null
  receiptNum?: number | string | null
  remark?: string | null
  managerName?: string | null
  senderName?: string | null
  auditMemberName?: string | null
  createdName?: string | null
  items?: VoucherPrintSourceItem[]
  amountToChinese: (n: number) => string
}

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** Match sample: 50,000.00 (no ￥). Empty/blank for empty cells. */
export function formatClassicPrintAmount(value: unknown): string {
  if (value === null || value === undefined || value === '') return ''
  const n = Number(value)
  if (Number.isNaN(n)) return ''
  return n.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export function formatClassicPrintDate(dateStr: string | null | undefined): string {
  if (!dateStr) return ''
  const [y, m, d] = String(dateStr).split('-')
  if (!y || !m || !d) return String(dateStr)
  return `${y} 年 ${m} 月 ${d} 日`
}

/** Match sample: 记 12 号 */
export function formatClassicWordNum(
  wordHead: string | null | undefined,
  _voucherDate: string | null | undefined,
  wordNum: number | string | null | undefined,
): string {
  const head = wordHead || '记'
  const num = wordNum == null || wordNum === '' ? '' : String(wordNum)
  return `${head} ${num} 号`
}

function renderRows(page: VoucherPrintPage): string {
  return page.rows
    .map((row) => {
      const aux = row.auxLabel
        ? `<span class="aux">${escapeHtml(row.auxLabel)}</span>`
        : ''
      const subjectInner = row.isEmpty
        ? '&nbsp;'
        : `${escapeHtml(row.subjectLabel)}${aux}`
      const summary = row.isEmpty ? '&nbsp;' : escapeHtml(row.summary) || '&nbsp;'
      const debit = row.isEmpty ? '' : formatClassicPrintAmount(row.debitAmount)
      const credit = row.isEmpty ? '' : formatClassicPrintAmount(row.creditAmount)
      const lineNo = row.lineNo == null ? '' : String(row.lineNo)
      return `<tr>
          <td class="summary">${summary}</td>
          <td class="subject">${subjectInner}</td>
          <td class="col-debit">${debit}</td>
          <td class="col-credit">${credit}</td>
          <td class="col-no">${lineNo}</td>
        </tr>`
    })
    .join('\n')
}

function renderSheet(
  page: VoucherPrintPage,
  input: ClassicVoucherPrintInput,
  debitTotal: number,
  creditTotal: number,
  amountChinese: string,
): string {
  const title = page.isContinuation ? '记账凭证（续）' : '记账凭证'
  const pageLabel =
    page.pageCount > 1
      ? `<span>第 ${page.pageIndex} / 共 ${page.pageCount} 页</span>`
      : ''
  const tfootOnly = page.isLast
    ? `<tfoot>
        <tr>
          <td class="label">合　计</td>
          <td class="label-cn">
            <span class="cn-tag">人民币（大写）</span>
            <span class="cn-amount">${escapeHtml(amountChinese)}</span>
          </td>
          <td class="col-debit">${formatClassicPrintAmount(debitTotal)}</td>
          <td class="col-credit">${formatClassicPrintAmount(creditTotal)}</td>
          <td></td>
        </tr>
        <tr class="remark-row">
          <td class="label">备　注</td>
          <td colspan="4" class="remark-value">${escapeHtml(input.remark || '')}</td>
        </tr>
      </tfoot>`
    : ''

  const signs = page.isLast
    ? `<div class="signs">
        <span>会计主管：<em>${escapeHtml(input.managerName || '')}</em></span>
        <span>过账：<em>${escapeHtml(input.senderName || '')}</em></span>
        <span>复核：<em>${escapeHtml(input.auditMemberName || '')}</em></span>
        <span>制单：<em>${escapeHtml(input.createdName || '')}</em></span>
      </div>`
    : ''

  return `<div class="sheet">
    <div class="title">${title}</div>
    <div class="meta-date">${escapeHtml(formatClassicPrintDate(input.voucherDate))}</div>
    <div class="meta-row">
      <div>核算单位：${escapeHtml(input.companyName || '')}</div>
      <div class="right">
        ${pageLabel}
        <span>${escapeHtml(formatClassicWordNum(input.wordHead, input.voucherDate, input.wordNum))}</span>
        <span>附件 ${escapeHtml(input.receiptNum ?? 0)} 张</span>
      </div>
    </div>
    <table class="voucher">
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
        ${renderRows(page)}
      </tbody>
      ${tfootOnly}
    </table>
    ${signs}
  </div>`
}

export function buildClassicVoucherPrintDocument(input: ClassicVoucherPrintInput): string {
  const pages = chunkVoucherPrintPages(input.items || [])
  const debitTotal = (input.items || []).reduce(
    (sum, item) => sum + (Number(item.debitAmount) || 0),
    0,
  )
  const creditTotal = (input.items || []).reduce(
    (sum, item) => sum + (Number(item.creditAmount) || 0),
    0,
  )
  const amountChinese = input.amountToChinese(debitTotal)
  const sheets = pages
    .map((page) => renderSheet(page, input, debitTotal, creditTotal, amountChinese))
    .join('\n')

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>凭证打印-${escapeHtml(formatClassicWordNum(input.wordHead, input.voucherDate, input.wordNum))}</title>
  <style>${CLASSIC_VOUCHER_PRINT_CSS}</style>
</head>
<body>
${sheets}
</body>
</html>`
}
