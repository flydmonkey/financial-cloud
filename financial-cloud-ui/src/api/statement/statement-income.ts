import request from '@/utils/Request'

// 利润表
export function selectIncome(query : any): any {
    return request({
        url: '/statement/income',
        method: 'get',
        params: query
    })
}

// 导出利润表
export function incomeExport(query : any): any {
    return request({
        url: '/statement/income/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

export function incomeExportPdf(query : any): any {
    return request({
        url: '/statement/income/export-pdf',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}

// 获取净利润
export function getNetProfit(query : any): any {
    return request({
        url: '/statement/net-profit',
        method: 'get',
        params: query
    })
}


// 利润表-生成
export function generateIncome(query : any): any {
    return request({
        url: '/statement/generateIncomeStatement',
        method: 'get',
        params: query
    })
}

// 利润表-删除
export function deleteIncome(query : any): any {
    return request({
        url: '/statement/deleteIncome',
        method: 'get',
        params: query
    })
}

// 利润表行次下钻（科目构成）
export function incomeDrill(query: any): any {
    return request({
        url: '/statement/income/drill',
        method: 'get',
        params: query
    })
}
