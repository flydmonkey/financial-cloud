import request from '@/utils/Request'

export function getGeneralLedger(query: any): any {
    return request({
        url: '/statement/general-ledger',
        method: 'get',
        params: query
    })
}

export function generalLedgerExport(query: any): any {
    return request({
        url: '/statement/general-ledger/export',
        method: 'get',
        params: query,
        responseType: 'blob'
    })
}
