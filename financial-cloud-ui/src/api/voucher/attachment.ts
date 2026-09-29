import request from '@/utils/Request'

/**
 * 凭证附件（影像）
 */

// 附件列表
export function listAttachments(voucherId: string): any {
    return request({
        url: '/voucher/attachment/list',
        method: 'get',
        params: {voucherId}
    })
}

// 上传附件（multipart）
export function uploadAttachment(voucherId: string, file: File): any {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('voucherId', voucherId)
    return request({
        url: '/voucher/attachment/upload',
        method: 'post',
        data: formData,
        headers: {'Content-Type': 'multipart/form-data'},
        timeout: 120000
    })
}

// 删除附件
export function deleteAttachment(id: string): any {
    return request({
        url: `/voucher/attachment/${id}`,
        method: 'delete'
    })
}

// 附件下载地址（流式）
export function attachmentDownloadUrl(id: string): string {
    return `/api/voucher/attachment/download/${id}`
}
