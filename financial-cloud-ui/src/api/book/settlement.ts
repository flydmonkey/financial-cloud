import request from '@/utils/Request'

// 查询结账月份
export function fetch(query : any): any {
    return request({
        url: '/settlement/fetch',
        method: 'get',
        params: query
    })
}

export function checkout(query : any): any {
    return request({
        url: '/settlement/checkout',
        method: 'get',
        params: query
    })
}

/** 反结账：仅最近已结月；yearPeriod 须等于当前账期上一月 */
export function uncheckout(yearPeriod?: string): any {
    return request({
        url: '/settlement/uncheckout',
        method: 'post',
        params: yearPeriod ? { yearPeriod } : undefined,
        data: yearPeriod ? { yearPeriod } : {}
    })
}

export function verify(options?: { silentError?: boolean }): any {
    return request({
        url: '/settlement/verify',
        method: 'get',
        silentError: options?.silentError === true,
    })
}


export function saveOne(data : any): any {
    return request({
        url: '/settlement/save',
        method: 'post',
        data: data
    })
}

export function updateOne(data : any): any {
    return request({
        url: '/settlement/update',
        method: 'put',
        data: data
    })
}

export function getOne(id : any): any {
    return request({
        url: `/settlement/get/${id}`,
        method: 'get',
    })
}

export function deleteBatch(data : any): any {
    return request({
        url: '/settlement/delete',
        method: 'delete',
        data: data
    })
}

export function fetchcarry(query : any): any {
    return request({
        url: '/settlementcarry/fetchcarry',
        method: 'get',
        params: query
    })
}

export function generateVoucherSubmit(data: any, options?: { silentError?: boolean }) {
    return request({
        url: '/settlementcarry/generate-voucher',
        method: 'post',
        data: data,
        silentError: options?.silentError === true,
    })
  }

export function deleteVoucherSubmit(data: any) {
    return request({
        url: '/settlementcarry/generate-voucher',
        method: 'post',
        data: data
    })
  }


  export function deleteOne(id : any): any {
    return request({
        url: `/settlementcarry/delete/${id}`,
        method: 'delete',
    })
}