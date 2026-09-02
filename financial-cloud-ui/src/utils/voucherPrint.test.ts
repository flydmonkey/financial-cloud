import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  VOUCHER_PRINT_PAGE_SIZE,
  buildAuxLabel,
  buildSubjectLabel,
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

describe('buildSubjectLabel', () => {
  it('uses the subject code and selected subject name instead of detailed accounts', () => {
    assert.equal(
      buildSubjectLabel({
        subjectCode: '1001',
        subjectName: '库存现金',
        displayName: '现金',
        detailedAccounts: '客户：华东贸易',
      }),
      '1001 库存现金',
    )
  })

  it('falls back to displayName, then to the subject code only', () => {
    assert.equal(buildSubjectLabel({ subjectCode: '1002', displayName: '银行存款' }), '1002 银行存款')
    assert.equal(buildSubjectLabel({ subjectCode: '1003', detailedAccounts: '辅助明细' }), '1003')
  })
})

describe('buildAuxLabel', () => {
  it('matches the grouped auxiliary labels shown in voucher edit mode', () => {
    assert.equal(
      buildAuxLabel({
        auxiliary: [
          { id: 'customer', label: '客户', value: [{ label: '华东贸易', value: '1' }] },
          { id: 'department', label: '部门', value: [{ label: '销售部', value: '2' }] },
        ],
      }),
      '辅助：客户:华东贸易；部门:销售部',
    )
  })

  it('omits empty auxiliary groups', () => {
    assert.equal(
      buildAuxLabel({
        auxiliary: [
          { id: 'customer', label: '客户', value: [] },
          { id: 'department', label: '部门', value: [{ label: '', value: '' }] },
        ],
      }),
      '',
    )
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
    assert.equal(pages[0].rows[2].lineNo, 3)
    assert.equal(pages[0].rows[5].lineNo, 6)
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
