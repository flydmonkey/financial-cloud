import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  canClickVoucherDraft,
  canClickVoucherSave,
  findNeighborVoucherIds,
  formatShortVoucherWord,
  formatVoucherStatusLabel,
  resolveLeafSubjectCode,
  sortVouchersForNavigation,
  snapshotVoucherEditable,
} from './voucherWorkspace.ts'

describe('voucherWorkspace navigation', () => {
  it('orders by date then wordHead then wordNum', () => {
    const sorted = sortVouchersForNavigation([
      { id: '3', voucherDate: '2026-08-02', wordHead: '记', wordNum: 1 },
      { id: '1', voucherDate: '2026-08-01', wordHead: '记', wordNum: 2 },
      { id: '2', voucherDate: '2026-08-01', wordHead: '记', wordNum: 1 },
    ])
    assert.deepEqual(
      sorted.map((item) => item.id),
      ['2', '1', '3'],
    )
  })

  it('finds previous and next neighbors', () => {
    const items = [
      { id: 'a', voucherDate: '2026-08-01', wordHead: '记', wordNum: 1 },
      { id: 'b', voucherDate: '2026-08-01', wordHead: '记', wordNum: 2 },
      { id: 'c', voucherDate: '2026-08-02', wordHead: '记', wordNum: 1 },
    ]
    assert.deepEqual(findNeighborVoucherIds(items, 'b'), { prevId: 'a', nextId: 'c' })
    assert.deepEqual(findNeighborVoucherIds(items, 'a'), { prevId: null, nextId: 'b' })
    assert.deepEqual(findNeighborVoucherIds(items, 'c'), { prevId: 'b', nextId: null })
  })

  it('finds neighbors for a new unsaved voucher by date+word position', () => {
    const items = [
      { id: 'a', voucherDate: '2026-08-01', wordHead: '记', wordNum: 1 },
      { id: 'b', voucherDate: '2026-08-01', wordHead: '记', wordNum: 2 },
      { id: 'c', voucherDate: '2026-08-02', wordHead: '记', wordNum: 1 },
    ]
    // 字号未分配：视为末尾，上一张=最后一张
    assert.deepEqual(
      findNeighborVoucherIds(items, null, { voucherDate: '2026-08-02', wordHead: '记', wordNum: null }),
      { prevId: 'c', nextId: null },
    )
    // 新建在末尾：上一张=c，下一张无
    assert.deepEqual(
      findNeighborVoucherIds(items, null, { voucherDate: '2026-08-02', wordHead: '记', wordNum: 2 }),
      { prevId: 'c', nextId: null },
    )
    // 新建插在 08-01 记2 与 08-02 记1 之间：上一张=b，下一张=c
    assert.deepEqual(
      findNeighborVoucherIds(items, null, { voucherDate: '2026-08-01', wordHead: '记', wordNum: 3 }),
      { prevId: 'b', nextId: 'c' },
    )
    // 新建在最前：上一张无，下一张=a
    assert.deepEqual(
      findNeighborVoucherIds(items, null, { voucherDate: '2026-07-31', wordHead: '记', wordNum: 1 }),
      { prevId: null, nextId: 'a' },
    )
  })
})

describe('voucherWorkspace display helpers', () => {
  it('formats short word and status', () => {
    assert.equal(formatShortVoucherWord('记', 1), '记 1 号')
    assert.equal(formatVoucherStatusLabel('draft'), '草稿')
    assert.equal(formatVoucherStatusLabel('completed', 'u1'), '已过账')
  })

  it('gates draft/save by status and period', () => {
    assert.equal(canClickVoucherDraft(null), true)
    assert.equal(canClickVoucherDraft('draft'), true)
    assert.equal(canClickVoucherDraft('completed'), false)
    assert.equal(canClickVoucherDraft('reviewing'), false)
    assert.equal(canClickVoucherDraft('draft', 'u1'), false)

    assert.equal(canClickVoucherSave('draft', null, '2026-09-01', '2026-09'), true)
    assert.equal(canClickVoucherSave(null, null, '2026-09-15', '2026-09'), true)
    assert.equal(canClickVoucherSave('draft', null, '2026-08-01', '2026-09'), false)
    assert.equal(canClickVoucherSave('completed', null, '2026-09-01', '2026-09'), false)
    assert.equal(canClickVoucherSave('draft', 'u1', '2026-09-01', '2026-09'), false)
  })

  it('gates draft/save by empty entries and loan balance', () => {
    const empty: any[] = [{ summary: '', subjectCode: '', debitAmount: '', creditAmount: '' }]
    const noAmount = [
      { summary: 'a', subjectCode: '1001', debitAmount: '', creditAmount: '' },
      { summary: 'b', subjectCode: '2001', debitAmount: '', creditAmount: '' },
    ]
    const unbalanced = [
      { summary: 'a', subjectCode: '1001', debitAmount: 100, creditAmount: 0 },
      { summary: 'b', subjectCode: '2001', debitAmount: 0, creditAmount: 50 },
    ]
    const balanced = [
      { summary: 'a', subjectCode: '1001', debitAmount: 100, creditAmount: 0 },
      { summary: 'b', subjectCode: '2001', debitAmount: 0, creditAmount: 100 },
    ]

    assert.equal(canClickVoucherDraft('draft', null, empty), false)
    assert.equal(canClickVoucherDraft('draft', null, noAmount), true)
    assert.equal(canClickVoucherDraft('draft', null, unbalanced), true)
    assert.equal(canClickVoucherSave('draft', null, '2026-09-01', '2026-09', empty), false)
    assert.equal(canClickVoucherSave('draft', null, '2026-09-01', '2026-09', noAmount), false)
    assert.equal(canClickVoucherSave('draft', null, '2026-09-01', '2026-09', unbalanced), false)
    assert.equal(canClickVoucherSave('draft', null, '2026-09-01', '2026-09', balanced), true)
  })

  it('detects dirty via snapshot', () => {
    const base = snapshotVoucherEditable({
      id: '1',
      wordHead: '记',
      wordNum: 1,
      voucherDate: '2026-08-01',
      items: [{ summary: 'a', subjectCode: '1001', debitAmount: 1 }],
    })
    const dirty = snapshotVoucherEditable({
      id: '1',
      wordHead: '记',
      wordNum: 1,
      voucherDate: '2026-08-01',
      items: [{ summary: 'b', subjectCode: '1001', debitAmount: 1 }],
    })
    assert.notEqual(base, dirty)
  })

  it('resolves leaf subject code for Enter commit', () => {
    const map = {
      '1001': { code: '1001', children: [] },
      '1002': { code: '1002', children: [{ code: '100201' }, { code: '100202' }] },
      '100201': { code: '100201', children: [] },
      '100202': { code: '100202', children: [] },
      '1122': { code: '1122', children: [] },
      '1131': { code: '1131', children: [] },
    }
    assert.equal(resolveLeafSubjectCode(map, '1001'), '1001')
    assert.equal(resolveLeafSubjectCode(map, '100201'), '100201')
    assert.equal(resolveLeafSubjectCode(map, '1002'), null)
    assert.equal(resolveLeafSubjectCode(map, '10020'), null)
    assert.equal(resolveLeafSubjectCode(map, '11'), null)
  })
})
