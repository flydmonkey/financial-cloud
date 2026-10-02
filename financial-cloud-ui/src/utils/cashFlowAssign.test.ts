import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  defaultCashFlowBalance,
  isCashFlowAssignmentBalanced,
  signedCashFlowForBalanceCheck,
} from './cashFlowAssign.ts'

describe('cashFlowAssign', () => {
  it('stores positive absolute amounts from debit/credit', () => {
    assert.equal(defaultCashFlowBalance({ debitAmount: 0, creditAmount: 50000 }), 50000)
    assert.equal(defaultCashFlowBalance({ debitAmount: 10000, creditAmount: 0 }), 10000)
  })

  it('signs outflows only for balance check (dir=1)', () => {
    assert.equal(signedCashFlowForBalanceCheck(50000, 2), 50000)
    assert.equal(signedCashFlowForBalanceCheck(10000, 1), -10000)
  })

  it('accepts +50000 for sales receipt (dir=2) on credit AR line', () => {
    const rows = [
      {
        entryNo: 1,
        debitAmount: 0,
        creditAmount: 50000,
        cashFlowItemCode: '2-jy-sqxj',
        cashFlowBalance: 50000,
      },
    ]
    const dirs = new Map<string, number>([['2-jy-sqxj', 2]])
    assert.equal(isCashFlowAssignmentBalanced(rows, dirs), true)
  })

  it('rejects negative sales receipt that previously passed buggy UI check', () => {
    const rows = [
      {
        entryNo: 1,
        debitAmount: 0,
        creditAmount: 50000,
        cashFlowItemCode: '2-jy-sqxj',
        cashFlowBalance: -50000,
      },
    ]
    const dirs = new Map<string, number>([['2-jy-sqxj', 2]])
    assert.equal(isCashFlowAssignmentBalanced(rows, dirs), false)
  })

  it('accepts +10000 for operating outflow (dir=1) on debit expense line', () => {
    const rows = [
      {
        entryNo: 1,
        debitAmount: 10000,
        creditAmount: 0,
        cashFlowItemCode: '9-jy-zfqt',
        cashFlowBalance: 10000,
      },
    ]
    const dirs = new Map<string, number>([['9-jy-zfqt', 1]])
    assert.equal(isCashFlowAssignmentBalanced(rows, dirs), true)
  })
})
