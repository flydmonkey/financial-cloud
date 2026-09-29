import request from '@/utils/Request'

// 查询初始余额列表
export function listBookInitBalance(query: any) {
    return request({
        url: '/base/init-balance/list',
        method: 'get',
        params: query
    })
}

// 保存初始余额
export function saveBookInitBalance(data: any) {
    return request({
        url: '/base/init-balance/save',
        method: 'post',
        data: data
    })
}

export function exportBookInitBalance(query: any) {
    return request({
        url: '/base/init-balance/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

export function downloadBookInitBalanceTemplate() {
    return request({
        url: '/base/init-balance/import-template',
        method: 'get',
        responseType: 'blob'
    })
}

export function importBookInitBalance(data: FormData) {
    return request({
        url: '/base/init-balance/import',
        method: 'post',
        data,
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}
