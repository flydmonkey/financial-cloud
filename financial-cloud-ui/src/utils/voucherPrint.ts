export const VOUCHER_PRINT_PAGE_SIZE = 6

type VoucherPrintAuxiliaryValue = {
  label?: string
  value?: string
}

type VoucherPrintAuxiliary = {
  id?: string
  itemName?: string
  name?: string
  label?: string
  value?: string | Array<VoucherPrintAuxiliaryValue | string>
}

export type VoucherPrintSourceItem = {
  summary?: string
  subjectCode?: string
  subjectName?: string
  displayName?: string
  detailedAccounts?: string
  debitAmount?: number | string | null
  creditAmount?: number | string | null
  auxiliary?: Array<VoucherPrintAuxiliary | string>
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
  const code = (item.subjectCode || '').trim()
  const name = (item.subjectName || item.displayName || '').trim()
  return `${code} ${name}`.trim()
}

export function buildAuxLabel(item: VoucherPrintSourceItem): string {
  const aux = item.auxiliary
  if (!aux || !Array.isArray(aux) || aux.length === 0) return ''
  const parts = aux
    .map((a) => {
      if (typeof a === 'string') return a
      if (Array.isArray(a.value)) {
        const values = a.value
          .map((value) => (typeof value === 'string' ? value : value.label || value.value || ''))
          .filter(Boolean)
        if (values.length === 0) return ''
        const groupLabel = a.label || a.itemName || a.name || ''
        return groupLabel ? `${groupLabel}:${values.join(',')}` : values.join(',')
      }
      return a.itemName || a.name || a.label || a.value || ''
    })
    .filter(Boolean)
  return parts.length ? `辅助：${parts.join('；')}` : ''
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
