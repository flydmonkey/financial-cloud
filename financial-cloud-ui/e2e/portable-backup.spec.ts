import {expect, test} from '@playwright/test'
import {execFileSync} from 'node:child_process'
import {randomBytes} from 'node:crypto'
import {getCurrentUser, getCurrentTerm, loginViaApi} from './helpers/auth'
import {createAndPostVoucher, getVoucherDetail} from './helpers/voucher'
import {fetchSubjectBalances, fetchBalanceSheet, getIncomeNetProfit} from './helpers/reports'
import {clearBooksViaScript, setupE2eBookViaApi} from './helpers/books'

test('v2 backup recovers posted accounting and attachment after source file loss', async ({request}) => {
    test.setTimeout(120_000)
    expect(process.env.FC_DB_NAME).toMatch(/^financial_cloud_e2e_/)
    let {headers} = await loginViaApi(request)
    const bookId = (await getCurrentUser(request, headers)).bookId
    const term = await getCurrentTerm(request, headers, bookId)
    const voucher = await createAndPostVoucher(request, headers, bookId, '便携备份恢复金额', 1234)
    // Incompressible >4MiB fixture exercises both attachment upload and backup restore transport.
    const bytes = Buffer.concat([Buffer.from('%PDF-portable-attachment-1234'), randomBytes(5 * 1024 * 1024)])
    const uploaded = await (await request.post(`/api/voucher/attachment/upload?voucherId=${voucher.voucherId}`,
        {headers, multipart: {file: {name: 'portable-invoice.pdf', mimeType: 'application/pdf', buffer: bytes}}})).json()
    expect(uploaded.code, uploaded.message).toBe(0)
    const balances = await fetchSubjectBalances(request, headers, term)
    const sheet = await fetchBalanceSheet(request, headers, term)
    const profit = await getIncomeNetProfit(request, headers, term)
    const exported = await request.post(`/api/book/backup/export?bookId=${bookId}`, {headers})
    expect(exported.headers()['content-type']).toContain('zip')
    const buffer = await exported.body()
    expect(buffer.length).toBeGreaterThan(4 * 1024 * 1024)
    const beforeBooks = (await (await request.get('/api/book/fetchAll', {headers})).json()).data
    const corrupt = JSON.parse(execFileSync(process.platform === 'win32' ? 'python' : 'python3',
        ['-X', 'utf8', 'tools/isolation_formats.py'], {cwd: '..', encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
            input: JSON.stringify({mode: 'corrupt-binary', file: buffer.toString('base64')})}))
    const rejected = await (await request.post('/api/book/backup/restore', {headers,
        multipart: {file: {name: 'corrupted.zip', mimeType: 'application/zip', buffer: Buffer.from(corrupt.file, 'base64')}}})).json()
    expect(rejected.code).not.toBe(0)
    expect(rejected.message).toContain('附件校验失败')
    expect((await (await request.get('/api/book/fetchAll', {headers})).json()).data).toEqual(beforeBooks)
    const inspect = (mode: string) => JSON.parse(execFileSync(process.platform === 'win32' ? 'python' : 'python3',
        ['-X', 'utf8', 'tools/book_isolation_fixture.py', mode, bookId], {cwd: '..', encoding: 'utf8', maxBuffer: 32 * 1024 * 1024}))
    expect(inspect('snapshot').history_system_logs.some((row: {message_action: string; topic: string}) =>
        row.topic === '账套备份' && row.message_action === 'export'), 'successful export is audited').toBe(true)
    expect(inspect('remove-files').removed).toBeGreaterThan(0)
    const missing = await request.post(`/api/book/backup/export?bookId=${bookId}`, {headers})
    expect((await missing.json()).code, 'missing attachment must fail export').not.toBe(0)
    // Reset all book business data and receive the package as a freshly initialized book admin.
    clearBooksViaScript()
    headers = (await loginViaApi(request)).headers
    await setupE2eBookViaApi(request, headers, {name: '恢复接收空账套', enableDate: term})
    expect((await (await request.get(`/api/book/get/${bookId}`, {headers})).json()).code,
        'source book must no longer exist').not.toBe(0)
    const restored = await (await request.post('/api/book/backup/restore', {headers,
        multipart: {file: {name: 'portable.zip', mimeType: 'application/zip', buffer}}})).json()
    expect(restored.code, JSON.stringify(restored)).toBe(0)
    const clone = restored.data.bookId
    expect((await (await request.get(`/api/users/switchBook/${clone}`, {headers})).json()).code).toBe(0)
    const restoredBalances = await fetchSubjectBalances(request, headers, term)
    const money = (rows: typeof balances) => rows.map(row => ({code: row.subjectCode, balance: Number(row.balance)})).sort((a,b) => a.code.localeCompare(b.code))
    expect(money(restoredBalances)).toEqual(money(balances))
    expect(await getIncomeNetProfit(request, headers, term)).toEqual(profit)
    const restoredSheet = await fetchBalanceSheet(request, headers, term)
    expect(restoredSheet.items.assets.map(row => row.currentBalance)).toEqual(sheet.items.assets.map(row => row.currentBalance))
    expect(restoredSheet.items.liability.map(row => row.currentBalance)).toEqual(sheet.items.liability.map(row => row.currentBalance))
    const vouchers = await (await request.get('/api/voucher/fetch?pageNumber=1&pageSize=100', {headers})).json()
    expect(vouchers.data.total).toBe(1)
    const restoredVoucher = vouchers.data.records[0]
    expect(restoredVoucher.id).not.toBe(voucher.voucherId)
    expect(Number((await getVoucherDetail(request, headers, restoredVoucher.id)).debitAmount)).toBe(1234)
    const attachments = await (await request.get(`/api/voucher/attachment/list?voucherId=${restoredVoucher.id}`, {headers})).json()
    expect(attachments.code, attachments.message).toBe(0)
    expect(attachments.data).toHaveLength(1)
    const attachment = attachments.data[0]
    expect(attachment.fileId).not.toBe(uploaded.data.fileId)
    const downloaded = await request.get(`/api/voucher/attachment/download/${attachment.id}`, {headers})
    expect(downloaded.ok()).toBeTruthy()
    expect(await downloaded.body()).toEqual(bytes)
    // The new package can itself be exported and restored without live source ids.
    expect((await request.post(`/api/book/backup/export?bookId=${clone}`, {headers})).headers()['content-type']).toContain('zip')
})
