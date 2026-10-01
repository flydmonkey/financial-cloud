/** Matches backend {@code SubjectDisplayNameUtils.VOUCHER_SUMMARY_MAX} / DB varchar(64). */
export const VOUCHER_SUMMARY_MAX = 64

export const SUMMARY_TRUNCATED_TIP = '摘要已截断至 64 字'

export function truncateVoucherSummary(summary: string | null | undefined): {
  value: string
  truncated: boolean
} {
  const raw = summary == null ? '' : String(summary)
  if (raw.length <= VOUCHER_SUMMARY_MAX) {
    return {value: raw, truncated: false}
  }
  return {value: raw.slice(0, VOUCHER_SUMMARY_MAX), truncated: true}
}
