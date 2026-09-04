import request from '@/utils/Request'

// 获取
export function get(relatedId:string,id: string): any {
    return request({
        url: '/vouchertemplate/get',
        method: 'get',
        params: {
            relatedId:relatedId,
            id: id
        }
    })
}

// 获取；category<=0 时不按类型过滤
export function list(relatedId: string, category: number): any {
    const params: any = {
        relatedId: relatedId,
        pageNumber: 1,
        pageSize: 500,
    }
    if (category != null && category > 0) {
        params.category = category
    }
    return request({
        url: '/vouchertemplate/fetch',
        method: 'get',
        params
    })
}

// 设置
export function save(data: any): any {
    return request({
        url: '/vouchertemplate/save',
        method: 'post',
        data: data
    })
}

// 删除
export function del(id: string): any {
    return request({
        url: '/vouchertemplate/delete' ,
        method: 'delete',
        data: {
            listIds: [id]
        }
    })
}
