import request from '@/utils/Request'

// 凭证汇总
export function selectGroupVoucherSummary(query : any): any {
    return request({
        url: '/statement/voucher-summary',
        method: 'get',
        params: query
    })
}

// 导出凭证汇总
export function voucherSummaryExport(query : any): any {
    return request({
        url: '/statement/voucher-summary/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

// 科目余额查询
export function getSubjectBalance(query : any): any {
    return request({
        url: '/statement/subject-balance/get',
        method: 'get',
        params: query
    })
}

// 科目余额表
export function selectGroupSubjectBalance(query : any): any {
    return request({
        url: '/statement/subject-balance',
        method: 'get',
        params: query
    })
}

// 导出科目余额表
export function subjectBalanceExport(query : any): any {
    return request({
        url: '/statement/subject-balance/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

// 资产负债表
export function selectGroupBalanceSheet(query : any): any {
    return request({
        url: '/statement/balance-sheet',
        method: 'get',
        params: query
    })
}

// 导出资产负债表
export function balanceSheetExport(query : any): any {
    return request({
        url: '/statement/balance-sheet/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

// 现金流量表
export function selectCashFlowSheet(query : any): any {
    return request({
        url: '/statement/cash-flow',
        method: 'get',
        params: query
    })
}

// 导出现金流量表
export function cashFlowExport(query : any): any {
    return request({
        url: '/statement/cash-flow/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}
