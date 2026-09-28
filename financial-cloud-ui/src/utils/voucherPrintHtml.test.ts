import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  buildClassicVoucherPrintDocument,
  formatClassicPrintAmount,
  formatClassicWordNum,
  CLASSIC_VOUCHER_PRINT_CSS,
} from './voucherPrintHtml.ts'

describe('classic voucher print html', () => {
  it('embeds the classic sample stylesheet selectors', () => {
    assert.match(CLASSIC_VOUCHER_PRINT_CSS, /table\.voucher/)
    assert.match(CLASSIC_VOUCHER_PRINT_CSS, /\.sheet\s*\{/)
    assert.match(CLASSIC_VOUCHER_PRINT_CSS, /letter-spacing:\s*0\.55em/)
  })

  it('formats amounts like the HTML sample (no yen sign)', () => {
    assert.equal(formatClassicPrintAmount(50000), '50,000.00')
    assert.equal(formatClassicPrintAmount(''), '')
  })

  it('formats voucher word like the HTML sample', () => {
    assert.equal(formatClassicWordNum('记', '2026-03-15', 12), '记 12 号')
  })

  it('renders classic class names and six body rows', () => {
    const html = buildClassicVoucherPrintDocument({
      companyName: '示例科技有限公司',
      voucherDate: '2026-03-15',
      wordHead: '记',
      wordNum: 12,
      receiptNum: 3,
      remark: '银行回单已附',
      managerName: '王敏',
      senderName: '李强',
      auditMemberName: '赵倩',
      createdName: '陈晨',
      items: [
        {
          summary: '收到客户货款',
          subjectCode: '1002',
          subjectName: '银行存款',
          debitAmount: 50000,
          creditAmount: 0,
        },
        {
          summary: '收到客户货款',
          subjectCode: '1122',
          subjectName: '应收账款',
          debitAmount: 0,
          creditAmount: 50000,
        },
      ],
      amountToChinese: () => '伍万圆整',
    })
    assert.match(html, /class="sheet"/)
    assert.match(html, /class="title"/)
    assert.match(html, /<table class="voucher">/)
    assert.match(html, /class="signs"/)
    assert.match(html, /50,000\.00/)
    assert.doesNotMatch(html, /￥/)
    assert.equal((html.match(/<tbody>[\s\S]*?<\/tbody>/)![0].match(/<tr>/g) || []).length, 6)
  })
})
