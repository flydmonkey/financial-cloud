import request from '@/utils/Request'

export function fetchDepreciationDetail(params: any) {
  return request({ url: '/fixed-asset/report/depreciation-detail', method: 'get', params })
}

export function fetchDepreciationSummary(params: any) {
  return request({ url: '/fixed-asset/report/depreciation-summary', method: 'get', params })
}

export function exportDepreciationDetail(params: any) {
  return request({
    url: '/fixed-asset/report/depreciation-detail/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportDepreciationSummary(params: any) {
  return request({
    url: '/fixed-asset/report/depreciation-summary/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
