/**
 * Cash-flow assign drawer helpers.
 *
 * Stored cashFlowBalance is the absolute CF amount (positive for both inflows and
 * outflows), matching API /report aggregation. Config direction (1=借/outflow,
 * 2=贷/inflow) is applied only when checking that assigned amounts offset the
 * non-cash lines' debit−credit difference:
 *   difference + signed(balance) ≈ 0
 * where signed(balance) = direction===1 ? −balance : +balance.
 */

export type CashFlowAssignRow = {
  entryNo?: string | number
  debitAmount?: number | string | null
  creditAmount?: number | string | null
  cashFlowItemCode?: string | null
  cashFlowBalance?: number | string | null
}

export function toAmount(value: number | string | null | undefined): number {
  const n = parseFloat(String(value ?? 0))
  return Number.isFinite(n) ? n : 0
}

/** Absolute amount to store when user picks a CF item for a voucher line. */
export function defaultCashFlowBalance(row: CashFlowAssignRow): number {
  const debit = toAmount(row.debitAmount)
  const credit = toAmount(row.creditAmount)
  const abs = Math.abs(debit - credit)
  if (abs > 0) {
    return abs
  }
  return debit || credit || 0
}

/**
 * Sign applied for balance check / cash-impact offset only (not for storage).
 * Outflow items (direction 1) offset debit-side difference; inflows (2) keep +.
 */
export function signedCashFlowForBalanceCheck(
  balance: number,
  direction: number | string | null | undefined,
): number {
  const dir = Number(direction)
  return dir === 1 ? -balance : balance
}

export function sumUniqueEntryDebitCredit(rows: CashFlowAssignRow[]): {
  totalDebit: number
  totalCredit: number
} {
  const processed = new Set<string | number>()
  let totalDebit = 0
  let totalCredit = 0
  for (const row of rows) {
    const key = row.entryNo
    if (key === undefined || key === null || processed.has(key)) {
      continue
    }
    processed.add(key)
    totalDebit += toAmount(row.debitAmount)
    totalCredit += toAmount(row.creditAmount)
  }
  return { totalDebit, totalCredit }
}

export function sumSignedCashFlow(
  rows: CashFlowAssignRow[],
  directionByCode: Map<string, number | string> | Record<string, number | string>,
): number {
  const getDir = (code: string) =>
    directionByCode instanceof Map ? directionByCode.get(code) : directionByCode[code]

  let sum = 0
  for (const row of rows) {
    const code = row.cashFlowItemCode
    if (!code || code === 'no-select') {
      continue
    }
    sum += signedCashFlowForBalanceCheck(toAmount(row.cashFlowBalance), getDir(code))
  }
  return sum
}

export function isCashFlowAssignmentBalanced(
  rows: CashFlowAssignRow[],
  directionByCode: Map<string, number | string> | Record<string, number | string>,
  tolerance = 0.01,
): boolean {
  const { totalDebit, totalCredit } = sumUniqueEntryDebitCredit(rows)
  const difference = totalDebit - totalCredit
  const totalCashFlow = sumSignedCashFlow(rows, directionByCode)
  return Math.abs(difference + totalCashFlow) < tolerance
}
