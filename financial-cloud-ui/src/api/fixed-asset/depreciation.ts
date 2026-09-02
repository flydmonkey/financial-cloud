import request from '@/utils/Request'

export function getDepreciationStatus(params?: any) {
  return request({ url: '/fixed-asset/depreciation/status', method: 'get', params })
}

export function getDepreciationParams(params?: any) {
  return request({ url: '/fixed-asset/depreciation/params', method: 'get', params })
}

export function saveDepreciationParams(data: any) {
  return request({ url: '/fixed-asset/depreciation/params', method: 'put', data })
}

export function listDepreciationWork(params?: any) {
  return request({ url: '/fixed-asset/depreciation/work', method: 'get', params })
}

export function saveDepreciationWork(data: any[], yearPeriod?: string) {
  return request({
    url: '/fixed-asset/depreciation/work',
    method: 'put',
    params: yearPeriod ? { yearPeriod } : undefined,
    data
  })
}

export function accrueDepreciation(data?: any) {
  return request({ url: '/fixed-asset/depreciation/accrue', method: 'post', data: data || {} })
}
