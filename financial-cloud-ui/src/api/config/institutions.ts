import request from '@/utils/Request'

// 机构配置
export function institutionsGetCurrent(): any {
  return request({
    url: '/config/institutions/getCurrent',
    method: 'get'
  })
}

export function institutionsUpdateCurrent(data : any): any {
  return request({
    url: '/config/institutions/updateCurrent',
    method: 'put',
    data: data
  })
}


