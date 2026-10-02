import assert from 'node:assert/strict'
import { test } from 'node:test'
import { accountingGuide, closingCheckAction, previousYearPeriod, validYearPeriod } from './accountingGuide'

test('server check labels lead to the matching processing step', () => {
  for (const label of ['未完成凭证检查', '凭证号连续性检查', '凭证借贷方余额的检查']) {
    assert.equal(closingCheckAction(label).step, 1)
  }
  for (const label of ['结转收入', '结转成本费用（含主营业务成本）', '结转本年利润', '固定资产折旧']) {
    assert.equal(closingCheckAction(label).step, 2)
  }
  assert.equal(closingCheckAction('往来款项（应收应付/账龄）').path, '/arap/aging')
  assert.deepEqual(Object.keys(closingCheckAction('未知检查项')), ['suggestion'])
})

test('delivery period handles year boundaries and rejects invalid query values', () => {
  assert.equal(previousYearPeriod('2026-01'), '2025-12')
  assert.equal(previousYearPeriod('2026-10'), '2026-09')
  for (const value of ['2026-00', '2026-13', '2026-1', '2026-01-01', ['2026-01'], undefined]) {
    assert.equal(validYearPeriod(value), '')
    assert.equal(previousYearPeriod(value), '')
  }
})

test('guide preserves the five stages and existing workflow routes', () => {
  assert.equal(accountingGuide.length, 5)
  assert.equal(accountingGuide[1].links[0].path, '/config/initBalance/index')
  assert.equal(accountingGuide[3].links[0].path, '/settlement/settle-period')
})
