import request from '@/utils/Request'

export function listBookMembers(bookId: string): any {
    return request({
        url: '/book/members/list',
        method: 'get',
        params: { bookId }
    })
}

export function searchBookUsers(bookId: string, q: string): any {
    return request({
        url: '/book/members/search',
        method: 'get',
        params: { bookId, q }
    })
}

export function grantBookMember(data: { bookId: string; userId: string; roleId: string }): any {
    return request({
        url: '/book/members/grant',
        method: 'post',
        data
    })
}

export function revokeBookMember(bookId: string, userId: string): any {
    return request({
        url: '/book/members/revoke',
        method: 'delete',
        params: { bookId, userId }
    })
}
