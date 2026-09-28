import request from '@/utils/Request'

// 银行对账 / 余额调节表
export function getReconciliation(query: any): any {
  return request({
    url: '/journal/reconciliation',
    method: 'get',
    params: query
  })
}

export function saveStatement(data: any): any {
  return request({
    url: '/journal/reconciliation/statement',
    method: 'put',
    data: data
  })
}

export function markReconciled(data: any): any {
  return request({
    url: '/journal/reconciliation/mark',
    method: 'put',
    data: data
  })
}
