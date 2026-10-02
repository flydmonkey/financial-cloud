import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {assertReportsBalanced} from './helpers/reports'
import {prepareRequiredCarryForClose, tryCheckoutCurrentPeriod} from './helpers/settlement'
import {generateAndPostCarryByCode} from './helpers/settlement-carry'
import {createAndPostVoucher, fixVoucherNumbering, pickStandardBusinessSubjects} from './helpers/voucher'

test('later revenue blocks closing until a supplemental carry is posted', async ({request}) => {
    const {headers} = await loginViaApi(request)
    const user = await getCurrentUser(request, headers)
    expect(user?.bookId).toBeTruthy()
    const bookId = user.bookId
    const term = await getCurrentTerm(request, headers, bookId)
    const {bank, revenue} = pickStandardBusinessSubjects(await fetchBookSubjects(request, headers, bookId))
    expect(bank).toBeTruthy()
    expect(revenue).toBeTruthy()
    const pair = {debit: bank!, credit: revenue!}
    await createAndPostVoucher(request, headers, bookId, '回归-首次收入', 100, pair)
    await generateAndPostCarryByCode(request, headers, 'qm_jz_sr')
    await createAndPostVoucher(request, headers, bookId, '回归-结转后新增收入', 50, pair)
    await fixVoucherNumbering(request, headers)

    const verify = await (await request.get('/api/settlement/verify', {headers})).json()
    expect(verify.code).not.toBe(0)
    const incomeCheck = verify.data.find((item: any) => item.item === '损益结转-收入')
    expect(incomeCheck.result).toBe(false)
    expect(incomeCheck.reason).toContain('补充结转')
    const checkout = await tryCheckoutCurrentPeriod(request, headers, bookId)
    expect(checkout.code).not.toBe(0)
    expect(checkout.nextTerm).toBe(term)

    await prepareRequiredCarryForClose(request, headers, bookId)
    await fixVoucherNumbering(request, headers)
    const ready = await (await request.get('/api/settlement/verify', {headers})).json()
    expect(ready.code, JSON.stringify(ready.data?.filter((item: any) => item.hard !== false && !item.result))).toBe(0)
    await assertReportsBalanced(request, headers, term)
})

test('expense reversal carries on the opposite side and leaves reports balanced', async ({request}) => {
    const {headers} = await loginViaApi(request)
    const user = await getCurrentUser(request, headers)
    const bookId = user.bookId
    const term = await getCurrentTerm(request, headers, bookId)
    const {bank, expense, salesExpense} = pickStandardBusinessSubjects(await fetchBookSubjects(request, headers, bookId))
    expect(bank && expense && salesExpense).toBeTruthy()
    await createAndPostVoucher(request, headers, bookId, '回归-正常销售费用', 20,
        {debit: salesExpense!, credit: bank!})
    await createAndPostVoucher(request, headers, bookId, '回归-管理费用冲回', 50,
        {debit: bank!, credit: expense!})
    await generateAndPostCarryByCode(request, headers, 'qm_jz_cbfy')
    const ready = await (await request.get('/api/settlement/verify', {headers})).json()
    const costCheck = ready.data.find((item: any) => item.item === '损益结转-成本费用')
    expect(costCheck.result, costCheck.reason).toBe(true)
    await assertReportsBalanced(request, headers, term)
})
