/**
 * Voucher workspace navigation helpers (date + wordHead + wordNum order).
 */

export type VoucherNavItem = {
  id: string | number
  voucherDate?: string | null
  wordHead?: string | null
  wordNum?: number | string | null
}

export function voucherNavSortKey(item: VoucherNavItem): string {
  const date = String(item.voucherDate || '').slice(0, 10)
  const head = String(item.wordHead || '')
  const rawNum = item.wordNum
  const num =
    rawNum == null || rawNum === ''
      ? '99999999'
      : String(Number(rawNum)).padStart(8, '0')
  return `${date}\0${head}\0${num}`
}

export function sortVouchersForNavigation(items: VoucherNavItem[]): VoucherNavItem[] {
  return [...(items || [])].sort((a, b) =>
    voucherNavSortKey(a).localeCompare(voucherNavSortKey(b), 'en'),
  )
}

function hasAssignedWordNum(wordNum: number | string | null | undefined): boolean {
  if (wordNum == null || wordNum === '') return false
  const n = Number(wordNum)
  return Number.isFinite(n) && n > 0
}

export function findNeighborVoucherIds(
  items: VoucherNavItem[],
  currentId: string | number | null | undefined,
  position?: Pick<VoucherNavItem, 'voucherDate' | 'wordHead' | 'wordNum'> | null,
): { prevId: string | number | null; nextId: string | number | null } {
  const sorted = sortVouchersForNavigation(items).filter((item) => item.id != null && item.id !== '')

  if (currentId != null && currentId !== '') {
    const idx = sorted.findIndex((item) => String(item.id) === String(currentId))
    if (idx >= 0) {
      return {
        prevId: idx > 0 ? sorted[idx - 1].id : null,
        nextId: idx < sorted.length - 1 ? sorted[idx + 1].id : null,
      }
    }
  }

  // 新建/未落库
  if (!position) {
    return { prevId: null, nextId: null }
  }

  // 字号尚未分配：视为当前序列末尾，避免被当成「第 0 号」排到最前
  if (!hasAssignedWordNum(position.wordNum)) {
    return {
      prevId: sorted.length ? sorted[sorted.length - 1].id : null,
      nextId: null,
    }
  }

  const key = voucherNavSortKey({ id: '', ...position })
  let insertAt = sorted.findIndex((item) => voucherNavSortKey(item).localeCompare(key, 'en') >= 0)
  if (insertAt < 0) {
    insertAt = sorted.length
  }
  return {
    prevId: insertAt > 0 ? sorted[insertAt - 1].id : null,
    nextId: insertAt < sorted.length ? sorted[insertAt].id : null,
  }
}

/** Snapshot of user-editable fields for dirty detection. */
export function snapshotVoucherEditable(form: any): string {
  const items = (form?.items || []).map((item: any) => ({
    summary: item.summary ?? '',
    subjectCode: item.subjectCode ?? '',
    subjectId: item.subjectId ?? '',
    debitAmount: item.debitAmount ?? '',
    creditAmount: item.creditAmount ?? '',
    auxiliary: item.auxiliary ?? [],
  }))
  return JSON.stringify({
    id: form?.id ?? null,
    wordHead: form?.wordHead ?? '',
    wordNum: form?.wordNum ?? '',
    voucherDate: form?.voucherDate ?? '',
    companyName: form?.companyName ?? '',
    receiptNum: form?.receiptNum ?? '',
    remark: form?.remark ?? '',
    items,
  })
}

export function formatShortVoucherWord(
  wordHead: string | null | undefined,
  wordNum: number | string | null | undefined,
): string {
  const head = wordHead || '记'
  const num = wordNum == null || wordNum === '' ? '' : String(wordNum)
  return `${head} ${num} 号`
}

export function formatVoucherStatusLabel(status: string | null | undefined, senderId?: any): string {
  if (senderId) return '已过账'
  switch (status) {
    case 'draft':
      return '草稿'
    case 'reviewing':
      return '审核中'
    case 'completed':
      return '已审核'
    default:
      return status ? String(status) : '新建'
  }
}

/** 新建或草稿（未过账）才允许暂存 / 提交保存 */
export function isDraftEditableStatus(
  status: string | null | undefined,
  senderId?: any,
): boolean {
  if (senderId) return false
  return !status || status === 'draft'
}

function parseEntryAmount(value: any): number {
  if (value == null || value === '') return 0
  const n = Number(String(value).replace(/,/g, '').trim())
  return Number.isFinite(n) ? n : 0
}

/** 是否已有录入内容（摘要 / 科目 / 金额任一有值） */
export function hasVoucherEntryContent(items: any[] | null | undefined): boolean {
  return (items || []).some((item) => {
    if (String(item?.summary || '').trim()) return true
    if (item?.subjectId || item?.subjectCode) return true
    if (parseEntryAmount(item?.debitAmount) !== 0) return true
    if (parseEntryAmount(item?.creditAmount) !== 0) return true
    return false
  })
}

/** 借贷是否平衡（按分录金额合计，允许双方都为 0） */
export function isVoucherLoanBalanced(items: any[] | null | undefined): boolean {
  let debit = 0
  let credit = 0
  for (const item of items || []) {
    debit += parseEntryAmount(item?.debitAmount)
    credit += parseEntryAmount(item?.creditAmount)
  }
  return Math.abs(debit - credit) < 0.005
}

/** 是否已录入金额（借贷合计不全为 0） */
export function hasVoucherAmountContent(items: any[] | null | undefined): boolean {
  let total = 0
  for (const item of items || []) {
    total += Math.abs(parseEntryAmount(item?.debitAmount))
    total += Math.abs(parseEntryAmount(item?.creditAmount))
  }
  return total > 0.005
}

/** 暂存：新建/草稿，且已有录入（允许借贷不平衡） */
export function canClickVoucherDraft(
  status: string | null | undefined,
  senderId?: any,
  items?: any[] | null,
): boolean {
  if (!isDraftEditableStatus(status, senderId)) return false
  if (items !== undefined) return hasVoucherEntryContent(items)
  return true
}

/** 保存（提交）：新建/草稿、当期、有录入、有金额、且借贷平衡 */
export function canClickVoucherSave(
  status: string | null | undefined,
  senderId: any,
  voucherDate: string | null | undefined,
  currentTerm: string | null | undefined,
  items?: any[] | null,
): boolean {
  if (!isDraftEditableStatus(status, senderId)) return false
  if (!voucherDate || !currentTerm) return false
  if (!String(voucherDate).startsWith(String(currentTerm))) return false
  if (items !== undefined) {
    if (!hasVoucherEntryContent(items)) return false
    if (!hasVoucherAmountContent(items)) return false
    if (!isVoucherLoanBalanced(items)) return false
  }
  return true
}

/** Resolve a typed subject code for Enter-to-commit (exact leaf, else unique leaf prefix). */
export function resolveLeafSubjectCode(
  subjectMap: Record<string, any>,
  typed: string | null | undefined,
): string | null {
  const code = String(typed || '').trim()
  if (!code || !subjectMap) {
    return null
  }
  const isLeaf = (item: any) => !item?.children || item.children.length === 0
  const exact = subjectMap[code]
  if (exact && isLeaf(exact)) {
    return String(exact.code ?? code)
  }
  const leaves = Object.values(subjectMap).filter(
    (item: any) => isLeaf(item) && String(item?.code ?? '').startsWith(code),
  )
  if (leaves.length === 1) {
    return String(leaves[0].code)
  }
  return null
}
