import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  booksBoardBlockerPath,
  filterBooksBoardRows,
  sortBooksBoardRows,
  summarizeBooksBoardRows,
} from './booksBoard.ts'

describe('booksBoardBlockerPath', () => {
  it('routes behind and ready-close to settle wizard', () => {
    assert.equal(booksBoardBlockerPath('BEHIND'), '/settlement/settle-period')
    assert.equal(booksBoardBlockerPath('READY_CLOSE'), '/settlement/settle-period')
  })

  it('routes pack / voucher / depreciation', () => {
    assert.equal(booksBoardBlockerPath('READY_PACK'), '/settlement/settle-list')
    assert.equal(booksBoardBlockerPath('AUDIT'), '/voucher/voucher-index')
    assert.equal(booksBoardBlockerPath('POST'), '/voucher/voucher-index')
    assert.equal(booksBoardBlockerPath('DEPRECIATION'), '/fixed-asset/depreciation')
  })
})

describe('filterBooksBoardRows', () => {
  it('filters by close status', () => {
    const rows = [
      { bookId: '1', closeStatus: 'OPEN' },
      { bookId: '2', closeStatus: 'CLOSED' },
      { bookId: '3', closeStatus: 'BEHIND' },
    ]
    assert.deepEqual(filterBooksBoardRows(rows, 'BEHIND'), [rows[2]])
    assert.equal(filterBooksBoardRows(rows, 'ALL').length, 3)
  })
})

describe('sortBooksBoardRows', () => {
  it('orders behind before open before closed', () => {
    const sorted = sortBooksBoardRows([
      { bookName: '丙', closeStatus: 'CLOSED' },
      { bookName: '乙', closeStatus: 'OPEN' },
      { bookName: '甲', closeStatus: 'BEHIND' },
    ])
    assert.deepEqual(
      sorted.map((r) => r.bookName),
      ['甲', '乙', '丙'],
    )
  })
})

describe('summarizeBooksBoardRows', () => {
  it('counts close buckets and todos', () => {
    const summary = summarizeBooksBoardRows([
      { closeStatus: 'OPEN', pendingPostCount: 0 },
      { closeStatus: 'CLOSED', pendingPostCount: 0 },
      { closeStatus: 'BEHIND', pendingAuditCount: 1 },
      { closeStatus: 'CLOSED', pendingPostCount: 2 },
    ])
    assert.deepEqual(summary, { total: 4, open: 1, closed: 2, behind: 1, withTodo: 3 })
  })
})
