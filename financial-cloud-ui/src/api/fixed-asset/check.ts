import request from '@/utils/Request'

export function listFixedAssetCheck(query: any) {
  return request({ url: '/fixed-asset/check/fetch', method: 'get', params: query })
}

export function getFixedAssetCheck(id: string) {
  return request({ url: `/fixed-asset/check/get/${id}`, method: 'get' })
}

export function createFixedAssetCheck(data: any) {
  return request({ url: '/fixed-asset/check/create', method: 'post', data })
}

export function updateFixedAssetCheckItem(data: any) {
  return request({ url: '/fixed-asset/check/item', method: 'put', data })
}

export function completeFixedAssetCheck(id: string) {
  return request({ url: `/fixed-asset/check/complete/${id}`, method: 'put' })
}

export function disposeDeficitFixedAssetCheck(id: string) {
  return request({ url: `/fixed-asset/check/dispose-deficit/${id}`, method: 'put' })
}

export function deleteFixedAssetCheck(id: string) {
  return request({ url: `/fixed-asset/check/${id}`, method: 'delete' })
}
