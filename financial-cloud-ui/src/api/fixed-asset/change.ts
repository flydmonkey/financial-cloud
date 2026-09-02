import request from '@/utils/Request'

export function listFixedAssetChange(query: any) {
  return request({ url: '/fixed-asset/change/fetch', method: 'get', params: query })
}

export function saveFixedAssetChange(data: any) {
  return request({ url: '/fixed-asset/change/save', method: 'post', data })
}
