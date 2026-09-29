import request from '@/utils/Request'

export function getExpenseDetail(query: any): any {
    return request({
        url: '/statement/expense-detail',
        method: 'get',
        params: query
    })
}

export function expenseDetailExport(query: any): any {
    return request({
        url: '/statement/expense-detail/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

export function expenseDetailExportPdf(query: any): any {
    return request({
        url: '/statement/expense-detail/export-pdf',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}
