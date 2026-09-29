import request from '@/utils/Request'

export function getVoucherSettlementParams() {
  return request({
    url: '/config/voucher-settlement',
    method: 'get'
  })
}

export function saveVoucherSettlementParams(data: {
  voucherReviewed: number
  arapVerifyEnabled: boolean
}) {
  return request({
    url: '/config/voucher-settlement',
    method: 'put',
    data
  })
}
