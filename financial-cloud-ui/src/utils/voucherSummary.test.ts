import assert from 'node:assert/strict'
import {describe, it} from 'node:test'
import {
  SUMMARY_TRUNCATED_TIP,
  VOUCHER_SUMMARY_MAX,
  truncateVoucherSummary,
} from './voucherSummary.ts'

describe('truncateVoucherSummary', () => {
  it('keeps short summary unchanged', () => {
    const r = truncateVoucherSummary('费用报销 差旅')
    assert.equal(r.truncated, false)
    assert.equal(r.value, '费用报销 差旅')
  })

  it('truncates to 64 and flags tip copy', () => {
    const long = '费用报销 '.repeat(20)
    const r = truncateVoucherSummary(long)
    assert.equal(r.truncated, true)
    assert.equal(r.value.length, VOUCHER_SUMMARY_MAX)
    assert.equal(SUMMARY_TRUNCATED_TIP, '摘要已截断至 64 字')
  })

  it('treats null as empty', () => {
    const r = truncateVoucherSummary(null)
    assert.equal(r.truncated, false)
    assert.equal(r.value, '')
  })
})
