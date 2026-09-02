import request from '@/utils/Request'

export function listAssetCategory(query: any) {
  return request({ url: '/fixed-asset/category/fetch', method: 'get', params: query })
}

export function listAllAssetCategory() {
  return request({ url: '/fixed-asset/category/list', method: 'get' })
}

export function getAssetCategory(id: string) {
  return request({ url: '/fixed-asset/category/get/' + id, method: 'get' })
}

export function addAssetCategory(data: any) {
  return request({ url: '/fixed-asset/category/save', method: 'post', data })
}

export function updateAssetCategory(data: any) {
  return request({ url: '/fixed-asset/category/update', method: 'put', data })
}

export function delAssetCategory(data: any) {
  return request({ url: '/fixed-asset/category/delete', method: 'delete', data })
}
