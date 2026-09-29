import request from '@/utils/Request'

// 税费测算
export function taxEstimate(query: any): any {
    return request({
        url: '/tax-estimate',
        method: 'get',
        params: query
    })
}

// 增值税申报表（简版主表）
export function taxDeclaration(query: any): any {
    return request({
        url: '/tax-estimate/declaration',
        method: 'get',
        params: query
    })
}
