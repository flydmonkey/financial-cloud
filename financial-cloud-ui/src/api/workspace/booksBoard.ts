import request from '@/utils/Request'

export function fetchBooksBoard(query: {
  focusPeriod?: string
  onlyTodo?: boolean
  keyword?: string
}): any {
  return request({
    url: '/workspace/books-board',
    method: 'get',
    params: query
  })
}

export function exportMonthlyBooksPackForBook(query: {
  yearPeriod: string
  includeVoucherList: boolean
  bookId: string
}): any {
  return request({
    url: '/statement/books-pack/export',
    method: 'get',
    params: query,
    responseType: 'blob',
    silentError: true
  })
}

export function exportBooksPackBatch(body: {
  bookIds: string[]
  yearPeriod: string
  includeVoucherList: boolean
}): any {
  return request({
    url: '/statement/books-pack/export-batch',
    method: 'post',
    data: body,
    responseType: 'blob',
    silentError: true
  })
}
