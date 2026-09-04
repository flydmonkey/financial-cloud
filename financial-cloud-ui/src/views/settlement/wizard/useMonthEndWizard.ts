import { computed, ref } from 'vue'
import * as voucherApis from '@/api/voucher/voucher'
import * as settlementApi from '@/api/book/settlement'
import {
  accrueDepreciation as accrueDepreciationApi,
  getDepreciationStatus,
} from '@/api/fixed-asset/depreciation'
import bookStore from '@/store/modules/bookStore'

export type WizardStep = 0 | 1 | 2 | 3 | 4

export interface UnpostedVoucherRow {
  id: string
  word: string
  status: string
  voucherDate: string
  senderId?: string | null
  remark?: string
  debitAmount?: number | string | null
  creditAmount?: number | string | null
  receiptNum?: number | null
  carryForward?: string | null
}

export interface CarryRow {
  code: string
  name: string
  templateId?: string
  voucherId?: string | null
  voucherStatus?: string | null
  done: boolean // posted
  phase: 'missing' | 'draft' | 'posted' | 'na'
}

const REQUIRED_CARRY_CODES = ['qm_jz_sr', 'qm_jz_cbfy'] as const
const CBFY_NAME_FALLBACK = '结转成本费用（含主营业务成本）'

function isBlankSender(senderId: unknown): boolean {
  return senderId == null || String(senderId).trim() === ''
}

function isCancelled(status: unknown): boolean {
  return status === 'cancelled'
}

/** Match SettlementService.countUnpostedVouchers: not cancelled AND empty/null senderId */
export function isUnpostedVoucher(v: { status?: string | null; senderId?: string | null }): boolean {
  return !isCancelled(v?.status) && isBlankSender(v?.senderId)
}

function hardFailed(rows: any[]): boolean {
  return (rows || []).some(
    (row: any) => row.hard !== false && row.applicable !== false && row.result === false,
  )
}

function requiredCarryCodesForTerm(term: string): string[] {
  const codes: string[] = [...REQUIRED_CARRY_CODES]
  const month = String(term || '').slice(5, 7)
  if (month === '12') {
    codes.push('qm_jz_bnlr')
  }
  return codes
}

function carryDisplayName(code: string, rowName?: string | null): string {
  if (code === 'qm_jz_cbfy') {
    return rowName || CBFY_NAME_FALLBACK
  }
  return rowName || code
}

export function jumpTargetForItem(item: string): WizardStep {
  const text = String(item || '')
  if (text.includes('未过账') || text.includes('未完成凭证')) {
    return 1
  }
  if (text.includes('凭证号') || text.includes('断号') || text.includes('连续')) {
    return 1
  }
  if (text.includes('借贷') || text.includes('借贷方') || text.includes('平衡')) {
    return 1
  }
  if (text.includes('结转')) {
    return 2
  }
  if (text.includes('折旧')) {
    return 2
  }
  return 3
}

function currentTerm(): string {
  return String(bookStore().termCurrent || '')
}

/**
 * Chain draft→submit, reviewing→audit, completed-without-sender→sender.
 * After each stage, previously advanced ids join the next batch (wizard "一键过账").
 */
function firstItemSummary(v: any): string {
  const items = Array.isArray(v?.items) ? v.items : []
  for (const item of items) {
    const s = item?.summary != null ? String(item.summary).trim() : ''
    if (s) {
      return s
    }
  }
  return ''
}

function mapVoucherRow(v: any): UnpostedVoucherRow {
  const headerRemark = v.remark != null ? String(v.remark).trim() : ''
  return {
    id: String(v.id),
    word: String(v.word || `${v.wordHead || ''}-${v.wordNum ?? ''}`),
    status: String(v.status || ''),
    voucherDate: String(v.voucherDate || ''),
    senderId: v.senderId ?? null,
    // 凭证头 remark 常为空，摘要在分录行上（与凭证列表页一致优先头备注，否则取首条分录摘要）
    remark: headerRemark || firstItemSummary(v),
    debitAmount: v.debitAmount ?? null,
    creditAmount: v.creditAmount ?? null,
    receiptNum: v.receiptNum != null ? Number(v.receiptNum) : null,
    carryForward: v.carryForward != null ? String(v.carryForward) : null,
  }
}

function rejectedErrorMessage(rejected: UnpostedVoucherRow[]): Error {
  const labels = rejected.map((r) => r.word || r.id).join('、')
  return new Error(
    `存在被拒绝的凭证（${labels}），无法自动提交/审核/过账，请先打开编辑并重新提交后再操作`,
  )
}

/**
 * Chain draft→submit, reviewing→audit, completed-without-sender→sender.
 * After each stage, previously advanced ids join the next batch (wizard "一键过账").
 * Caller must exclude rejected rows (they cannot advance via batch APIs).
 */
async function chainSubmitAuditPost(
  rows: Array<{ id: string; status: string; senderId?: string | null }>,
): Promise<void> {
  const byId = new Map(rows.map((r) => [r.id, r]))
  const ids = [...byId.keys()]
  if (!ids.length) {
    return
  }

  const drafts = ids.filter((id) => byId.get(id)?.status === 'draft')
  const reviewing = ids.filter((id) => byId.get(id)?.status === 'reviewing')
  const completedUnposted = ids.filter((id) => {
    const row = byId.get(id)
    return row?.status === 'completed' && isBlankSender(row?.senderId)
  })

  if (drafts.length) {
    await voucherApis.submitBatch(drafts.join(','))
  }

  const toAudit = [...new Set([...reviewing, ...drafts])]
  if (toAudit.length) {
    await voucherApis.auditBatch(toAudit.join(','))
  }

  const toSender = [...new Set([...completedUnposted, ...toAudit])]
  if (toSender.length) {
    await voucherApis.senderBatch(toSender.join(','))
  }
}

/** Fetch all current-term vouchers (paginate until exhausted). */
async function listAllTermVouchers(year: string, month: number): Promise<any[]> {
  const pageSize = 200
  const all: any[] = []
  let pageNumber = 1
  // Safety cap: 200 * 50 = 10k vouchers/term
  const maxPages = 50
  while (pageNumber <= maxPages) {
    const res: any = await voucherApis.listVouchers({
      pageNumber,
      pageSize,
      voucherYear: year,
      voucherMonth: month,
      includeItems: true,
    })
    const records: any[] = res?.data?.records || []
    all.push(...records)
    const total = Number(res?.data?.total)
    if (Number.isFinite(total) && total >= 0 && all.length >= total) {
      break
    }
    if (records.length < pageSize) {
      break
    }
    pageNumber += 1
  }
  return all
}

export function useMonthEndWizard() {
  const active = ref<WizardStep>(0)
  const manualAck = ref(false)

  const unposted = ref<UnpostedVoucherRow[]>([])
  const successiveGaps = ref<any[]>([])
  const carryRows = ref<CarryRow[]>([])

  const deprNeeded = ref(true)
  const deprAccrued = ref(false)

  const verifyRows = ref<any[]>([])
  const isVerify = ref(false)
  const isCheckout = ref(false)
  const checkoutOk = ref(false)
  const checkoutError = ref('')

  const loadingStep1 = ref(false)
  const loadingStep2 = ref(false)
  const loadingVerify = ref(false)
  const loadingCheckout = ref(false)
  const loadingAction = ref(false)

  const canLeaveStep0 = computed(() => !!manualAck.value)
  const canLeaveStep1 = computed(
    () => unposted.value.length === 0 && successiveGaps.value.length === 0,
  )
  const canLeaveStep2 = computed(() => {
    const deprOk = !deprNeeded.value || deprAccrued.value
    const carriesOk =
      carryRows.value.length > 0 &&
      carryRows.value.every((r) => r.phase === 'posted' || r.phase === 'na')
    return deprOk && carriesOk
  })
  const canLeaveStep3 = computed(() => !!isVerify.value)

  async function refreshStep1(): Promise<void> {
    loadingStep1.value = true
    try {
      const term = currentTerm()
      const year = term.slice(0, 4)
      const month = Number(term.slice(5, 7))
      const records = await listAllTermVouchers(year, month)
      unposted.value = records.filter((v: any) => isUnpostedVoucher(v)).map(mapVoucherRow)

      const suc: any = await voucherApis.getVoucherSuccessiveList({})
      successiveGaps.value = suc?.data || []
    } finally {
      loadingStep1.value = false
    }
  }

  async function submitAuditPost(ids: string[]): Promise<void> {
    const requested = (ids || []).filter(Boolean).map(String)
    // Empty selection = batch all current unposted
    const idSet = new Set(requested.length ? requested : unposted.value.map((r) => r.id))
    if (!idSet.size) {
      return
    }
    loadingAction.value = true
    try {
      let rows = unposted.value.filter((r) => idSet.has(r.id))
      const missing = [...idSet].filter((id) => !rows.some((r) => r.id === id))
      if (missing.length) {
        const fetched: UnpostedVoucherRow[] = []
        for (const id of missing) {
          const res: any = await voucherApis.getOneVoucher(id)
          const v = res?.data
          if (v && isUnpostedVoucher(v)) {
            fetched.push(mapVoucherRow(v))
          }
        }
        rows = [...rows, ...fetched]
      }

      const rejected = rows.filter((r) => r.status === 'rejected')
      const processable = rows.filter((r) => r.status !== 'rejected')

      if (processable.length) {
        await chainSubmitAuditPost(processable)
      }
      await refreshStep1()

      if (rejected.length) {
        throw rejectedErrorMessage(rejected)
      }
    } finally {
      loadingAction.value = false
    }
  }

  async function fixSuccessive(): Promise<void> {
    loadingAction.value = true
    try {
      const suc: any = await voucherApis.getVoucherSuccessiveList({})
      const gaps = suc?.data || []
      successiveGaps.value = gaps
      if (gaps.length) {
        await voucherApis.updateVoucherSuccessive(gaps)
      }
      await refreshStep1()
    } finally {
      loadingAction.value = false
    }
  }

  async function refreshStep2(): Promise<void> {
    loadingStep2.value = true
    try {
      const term = currentTerm()
      const codes = requiredCarryCodesForTerm(term)

      try {
        const carryRes: any = await settlementApi.fetchcarry({
          pageNumber: 1,
          pageSize: 50,
          category: 1,
        })
        const records: any[] = carryRes?.data?.records || []
        const rows: CarryRow[] = []

        for (const code of codes) {
          const row = records.find((r: any) => r.code === code)
          if (!row) {
            const na = code === 'qm_jz_bnlr'
            rows.push({
              code,
              name: carryDisplayName(code),
              templateId: undefined,
              voucherId: null,
              voucherStatus: null,
              done: na,
              phase: na ? 'na' : 'missing',
            })
            continue
          }

          const templateId = String(row.id)
          const voucherId = row.voucherId ? String(row.voucherId) : null
          if (!voucherId) {
            rows.push({
              code,
              name: carryDisplayName(code, row.name),
              templateId,
              voucherId: null,
              voucherStatus: null,
              done: false,
              phase: 'missing',
            })
            continue
          }

          let voucherStatus: string | null = null
          let senderId: string | null = null
          try {
            const vRes: any = await voucherApis.getOneVoucher(voucherId)
            voucherStatus = vRes?.data?.status ?? null
            senderId = vRes?.data?.senderId ?? null
          } catch {
            voucherStatus = null
            senderId = null
          }

          const posted = !isBlankSender(senderId)
          rows.push({
            code,
            name: carryDisplayName(code, row.name),
            templateId,
            voucherId,
            voucherStatus,
            done: posted,
            phase: posted ? 'posted' : 'draft',
          })
        }
        carryRows.value = rows
      } catch {
        carryRows.value = []
      }

      try {
        const deprRes: any = await getDepreciationStatus({ yearPeriod: term })
        const data = deprRes?.data || {}
        if (data.needed === false) {
          deprNeeded.value = false
          deprAccrued.value = true
        } else {
          deprNeeded.value = true
          deprAccrued.value = !!data.accrued
        }
      } catch {
        deprNeeded.value = true
        deprAccrued.value = false
      }
    } finally {
      loadingStep2.value = false
    }
  }

  async function generateAndPostCarry(code: string): Promise<void> {
    loadingAction.value = true
    try {
      let row = carryRows.value.find((r) => r.code === code)
      if (!row || row.phase === 'na') {
        return
      }
      if (!row.templateId) {
        throw new Error(`缺少必做结转模板 ${code}`)
      }

      let voucherId = row.voucherId || null
      let status = row.voucherStatus || 'draft'
      let senderId: string | null = null

      if (!voucherId) {
        let gen: any
        try {
          gen = await settlementApi.generateVoucherSubmit(
            {
              id: row.templateId,
              templateId: row.templateId,
              voucherType: 1,
            },
            { silentError: true },
          )
        } catch (err: any) {
          const msg = String(err?.message || '')
          if (msg.includes('无需结转')) {
            carryRows.value = carryRows.value.map((r) =>
              r.code === code
                ? { ...r, done: true, phase: 'na' as const, voucherId: null, voucherStatus: null }
                : r,
            )
            return
          }
          throw new Error(msg || '生成结转凭证失败')
        }
        voucherId = gen?.data ? String(gen.data) : null
        if (!voucherId) {
          const msg = String(gen?.message || '')
          if (msg.includes('无需结转')) {
            carryRows.value = carryRows.value.map((r) =>
              r.code === code
                ? { ...r, done: true, phase: 'na' as const, voucherId: null, voucherStatus: null }
                : r,
            )
            return
          }
          throw new Error(msg || '生成结转凭证失败')
        }
        status = 'draft'
      } else {
        const vRes: any = await voucherApis.getOneVoucher(voucherId)
        status = String(vRes?.data?.status || status)
        senderId = vRes?.data?.senderId ?? null
        if (!isBlankSender(senderId)) {
          await refreshStep2()
          return
        }
        if (status === 'rejected') {
          throw new Error(
            `结转凭证（${code}）已被拒绝，无法自动过账，请先打开编辑并重新提交后再操作`,
          )
        }
      }

      await chainSubmitAuditPost([{ id: voucherId, status, senderId }])
      await refreshStep2()
    } finally {
      loadingAction.value = false
    }
  }

  async function accrueDepreciation(): Promise<void> {
    loadingAction.value = true
    try {
      await accrueDepreciationApi({ yearPeriod: currentTerm() })
      await refreshStep2()
    } finally {
      loadingAction.value = false
    }
  }

  async function runVerify(): Promise<void> {
    loadingVerify.value = true
    isVerify.value = false
    try {
      const res: any = await settlementApi.verify({ silentError: true })
      verifyRows.value = res?.data || []
      isVerify.value = res?.code === 0 && !hardFailed(verifyRows.value)
      const deprItem = verifyRows.value.find((i: any) => i.item === '固定资产折旧')
      if (deprItem) {
        deprNeeded.value = deprItem.applicable !== false
      }
    } catch (err: any) {
      verifyRows.value = err?.data || []
      isVerify.value = false
    } finally {
      loadingVerify.value = false
    }
  }

  async function checkout(): Promise<void> {
    if (!isVerify.value) {
      return
    }
    loadingCheckout.value = true
    checkoutError.value = ''
    try {
      const term = currentTerm()
      const year = term.substring(0, 4)
      const res: any = await settlementApi.checkout({ year, date: term })
      isCheckout.value = true
      if (res?.code === 0) {
        checkoutOk.value = true
        bookStore().getBookItem()
      } else {
        checkoutOk.value = false
        checkoutError.value = res?.message || ''
      }
    } catch (err: any) {
      isCheckout.value = true
      checkoutOk.value = false
      checkoutError.value = err?.message || err?.data?.message || ''
    } finally {
      loadingCheckout.value = false
    }
  }

  /** After checkout failure: return to verify step with footer actions restored. */
  function backToVerify(): void {
    isCheckout.value = false
    checkoutOk.value = false
    checkoutError.value = ''
    active.value = 3
  }

  return {
    active,
    manualAck,
    unposted,
    successiveGaps,
    carryRows,
    deprNeeded,
    deprAccrued,
    verifyRows,
    isVerify,
    isCheckout,
    checkoutOk,
    checkoutError,
    loadingStep1,
    loadingStep2,
    loadingVerify,
    loadingCheckout,
    loadingAction,
    canLeaveStep0,
    canLeaveStep1,
    canLeaveStep2,
    canLeaveStep3,
    refreshStep1,
    submitAuditPost,
    fixSuccessive,
    refreshStep2,
    generateAndPostCarry,
    accrueDepreciation,
    runVerify,
    checkout,
    backToVerify,
    jumpTargetForItem,
  }
}
