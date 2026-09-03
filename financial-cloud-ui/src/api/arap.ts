import request from '@/utils/Request'

export function fetchArapBalance(query: any) {
  return request({ url: '/arap/balance', method: 'get', params: query })
}

export function fetchArapDetail(query: any) {
  return request({ url: '/arap/detail', method: 'get', params: query })
}

export function fetchArapAging(query: any) {
  return request({ url: '/arap/aging', method: 'get', params: query })
}

export function exportArapStatement(query: any) {
  return request({
    url: '/arap/statement/export',
    method: 'get',
    params: query,
    responseType: 'blob',
  })
}
