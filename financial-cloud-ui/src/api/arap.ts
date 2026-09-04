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

export function fetchArapOpenItems(query: any) {
  return request({ url: '/arap/writeoff/open-items', method: 'get', params: query })
}

export function fetchArapWriteoffSuggest(query: any) {
  return request({ url: '/arap/writeoff/suggest', method: 'get', params: query })
}

export function confirmArapWriteoff(data: any) {
  return request({ url: '/arap/writeoff/confirm', method: 'post', data })
}

export function reverseArapWriteoff(id: string) {
  return request({ url: `/arap/writeoff/reverse/${id}`, method: 'post' })
}

export function fetchArapWriteoffList(query: any) {
  return request({ url: '/arap/writeoff/list', method: 'get', params: query })
}
