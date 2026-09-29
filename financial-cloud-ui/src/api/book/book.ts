import request from '@/utils/Request'

// 查询会计科目列表
export function listBooksSets(query : any): any {
    return request({
        url: '/book/fetch',
        method: 'get',
        params: query
    })
}

export function saveOne(data : any): any {
    return request({
        url: '/book/save',
        method: 'post',
        data: data
    })
}

export function updateOne(data : any): any {
    return request({
        url: '/book/update',
        method: 'put',
        data: data
    })
}

export function getOne(id : any): any {
    return request({
        url: `/book/get/${id}`,
        method: 'get',
    })
}

export function deleteBatch(data : any): any {
    return request({
        url: '/book/delete',
        method: 'delete',
        data: data
    })
}

export function listStore(): any {
    return request({
        url: '/book/fetchAll',
        method: 'get'
    })
}

export function getOnboardingStatus(): any {
    return request({
        url: '/book/onboarding-status',
        method: 'get'
    })
}

export function setupBook(data: any): any {
    return request({
        url: '/book/setup',
        method: 'post',
        data: data
    })
}


// 导出账套业务备份包（ZIP）
export function exportBookBackup(bookId: string): any {
    return request({
        url: '/book/backup/export',
        method: 'post',
        params: {bookId},
        responseType: 'blob',
        silentError: true
    })
}

// 上传备份包恢复为新账套
export function restoreBookBackup(file: File): any {
    const formData = new FormData()
    formData.append('file', file)
    return request({
        url: '/book/backup/restore',
        method: 'post',
        data: formData,
        headers: {'Content-Type': 'multipart/form-data'},
        timeout: 300000
    })
}

/** 定时备份状态 */
export function fetchBackupScheduleStatus(): any {
    return request({
        url: '/book/backup/schedule/status',
        method: 'get'
    })
}

/** 立即跑一轮定时备份 */
export function runBackupScheduleNow(): any {
    return request({
        url: '/book/backup/schedule/run',
        method: 'post',
        timeout: 600000
    })
}

// 封存账套（归档只读）
export function sealBook(id: string): any {
    return request({
        url: '/book/seal/' + id,
        method: 'put'
    })
}

// 解除封存
export function unsealBook(id: string): any {
    return request({
        url: '/book/unseal/' + id,
        method: 'put'
    })
}
