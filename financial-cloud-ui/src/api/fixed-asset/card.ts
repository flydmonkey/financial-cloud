import request from '@/utils/Request'

export function listFixedAsset(query: any) {
  return request({ url: '/fixed-asset/card/fetch', method: 'get', params: query })
}

export function getFixedAsset(id: string) {
  return request({ url: '/fixed-asset/card/get/' + id, method: 'get' })
}

export function addFixedAsset(data: any) {
  return request({ url: '/fixed-asset/card/save', method: 'post', data })
}

export function updateFixedAsset(data: any) {
  return request({ url: '/fixed-asset/card/update', method: 'put', data })
}

export function delFixedAsset(data: any) {
  return request({ url: '/fixed-asset/card/delete', method: 'delete', data })
}

export function disposeFixedAsset(id: string, data?: any) {
  return request({ url: '/fixed-asset/card/dispose/' + id, method: 'post', data: data || {} })
}

export function copyFixedAsset(id: string) {
  return request({ url: '/fixed-asset/card/copy/' + id, method: 'post' })
}

export function suspendFixedAsset(id: string) {
  return request({ url: '/fixed-asset/card/suspend/' + id, method: 'post' })
}

export function resumeFixedAsset(id: string) {
  return request({ url: '/fixed-asset/card/resume/' + id, method: 'post' })
}

export function exportFixedAsset(query: any) {
  return request({
    url: '/fixed-asset/card/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

export function downloadFixedAssetTemplate() {
  return request({
    url: '/fixed-asset/card/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

export function importFixedAsset(data: FormData) {
  return request({
    url: '/fixed-asset/card/import',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
