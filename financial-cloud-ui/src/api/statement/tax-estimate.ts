import request from '@/utils/Request'

// 税费测算
export function taxEstimate(query: any): any {
    return request({
        url: '/tax-estimate',
        method: 'get',
        params: query
    })
}
