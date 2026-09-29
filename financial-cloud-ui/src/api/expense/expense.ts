import request from '@/utils/Request'

// 费用报销分页
export function expenseClaimPage(query: any): any {
    return request({
        url: '/expense/claim',
        method: 'get',
        params: query
    })
}

// 新增/修改报销单
export function expenseClaimSave(data: any): any {
    return request({
        url: '/expense/claim',
        method: 'post',
        data
    })
}

// 提交审核
export function expenseClaimSubmit(id: string): any {
    return request({
        url: `/expense/claim/submit/${id}`,
        method: 'put'
    })
}

// 审核（approve=true 通过，false 拒绝）
export function expenseClaimAudit(id: string, approve: boolean, reason: string): any {
    return request({
        url: `/expense/claim/audit/${id}`,
        method: 'put',
        params: {approve, reason}
    })
}

// 一键生成报销凭证
export function expenseClaimVoucher(id: string): any {
    return request({
        url: `/expense/claim/voucher/${id}`,
        method: 'post'
    })
}

// 删除
export function expenseClaimDelete(id: string): any {
    return request({
        url: `/expense/claim/${id}`,
        method: 'delete'
    })
}
