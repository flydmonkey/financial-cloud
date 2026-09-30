import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertBalanceSheetLineByNameMatchesRulesFromConfig,
    assertBalanceSheetLineMatchesConfig,
    assertReportsBalanced,
    fetchBalanceSheet,
    findBalanceSheetItemByName,
    num,
} from './helpers/reports'
import {createAndPostVoucher, pickStandardBusinessSubjects} from './helpers/voucher'

/**
 * BS-R01~R04：往来重分类 + 坏账备抵（R04/R03 在 R01/R02 前，避免累计干扰）
 */
test.describe.serial('balance sheet reclassification', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
    } = {headers: {}, bookId: '', term: ''}

    test('login', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
    })

    test('BS-R04: bad debt allowance reduces 应收账款净值', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {receivable, revenue, badDebtAllowance, badDebtExpense} = pickStandardBusinessSubjects(subjects)
        test.skip(!receivable || !revenue, '缺少 1122/5001')
        test.skip(!badDebtAllowance || !badDebtExpense, '缺少 1141/5711 坏账科目')

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '坏账测-赊销', 20_000,
            {debit: receivable, credit: revenue},
        )
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '坏账测-计提准备', 3_000,
            {debit: badDebtExpense, credit: badDebtAllowance},
        )

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const assets = balanceSheet?.items?.assets || []
        const arLine = findBalanceSheetItemByName(assets, '应收账款')
        expect(arLine?.itemCode, '模板无应收账款行').toBeTruthy()
        expect(num(arLine?.currentBalance)).toBeCloseTo(17_000, 0)
        await assertBalanceSheetLineByNameMatchesRulesFromConfig(
            request, ctx.headers, ctx.term, '应收账款',
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('BS-R03: same-subject mixed direction uses net ledger balance', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, receivable, revenue} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !receivable || !revenue, '缺少 1002/1122/5001')

        const beforeSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const beforeAr = num(
            findBalanceSheetItemByName(beforeSheet?.items?.assets || [], '应收账款')?.currentBalance,
        )

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '混合-赊销', 10_000,
            {debit: receivable, credit: revenue},
        )
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '混合-预收冲账', 3_000,
            {debit: bank, credit: receivable},
        )

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const assets = balanceSheet?.items?.assets || []
        const liability = balanceSheet?.items?.liability || []
        const arLine = findBalanceSheetItemByName(assets, '应收账款')
        const advanceLine = findBalanceSheetItemByName(liability, '预收款项')

        // 本用例净增应收 7000；不依赖 R04 是否执行（共享账套用增量）
        expect(num(arLine?.currentBalance) - beforeAr).toBeCloseTo(7_000, 0)
        expect(num(advanceLine?.currentBalance)).toBeGreaterThanOrEqual(0)
        await assertBalanceSheetLineByNameMatchesRulesFromConfig(
            request, ctx.headers, ctx.term, '应收账款',
        )
        test.info().annotations.push({
            type: 'note',
            description: 'BS-R05 明细级重分类需辅助核算分行余额，当前为总账轧差口径',
        })
    })

    test('BS-R01: AR credit balance reclassifies to 预收款项', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, receivable} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !receivable, '缺少 1002/1122')

        const beforeSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const beforeAssets = beforeSheet?.items?.assets || []
        const beforeLiability = beforeSheet?.items?.liability || []
        const beforeAr = num(findBalanceSheetItemByName(beforeAssets, '应收账款')?.currentBalance)
        const beforeAdvance = num(findBalanceSheetItemByName(beforeLiability, '预收款项')?.currentBalance)

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '重分类-预收性质', 15_000,
            {debit: bank, credit: receivable},
        )

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const assets = balanceSheet?.items?.assets || []
        const liability = balanceSheet?.items?.liability || []
        const arLine = findBalanceSheetItemByName(assets, '应收账款')
        const advanceLine = findBalanceSheetItemByName(liability, '预收款项')
        test.skip(!advanceLine?.itemCode, '模板无预收款项行')

        const afterAr = num(arLine?.currentBalance)
        const afterAdvance = num(advanceLine?.currentBalance)
        // 贷记应收 15000：应收账款净值（应收−预收）应下降 15000
        expect((afterAr - afterAdvance) - (beforeAr - beforeAdvance)).toBeCloseTo(-15_000, 0)
        await assertBalanceSheetLineMatchesConfig(
            request, ctx.headers, ctx.term, String(advanceLine!.itemCode),
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('BS-R02: AP debit balance reclassifies to 预付款项', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, payable} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !payable, '缺少 1002/2202')

        const beforeSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const beforeAssets = beforeSheet?.items?.assets || []
        const beforeLiability = beforeSheet?.items?.liability || []
        const beforeAp = num(findBalanceSheetItemByName(beforeLiability, '应付账款')?.currentBalance)
        const beforePrepaid = num(findBalanceSheetItemByName(beforeAssets, '预付款项')?.currentBalance)

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '重分类-预付性质', 6_000,
            {debit: payable, credit: bank},
        )

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const assets = balanceSheet?.items?.assets || []
        const liability = balanceSheet?.items?.liability || []
        const apLine = findBalanceSheetItemByName(liability, '应付账款')
        const prepaidLine = findBalanceSheetItemByName(assets, '预付款项')
        test.skip(!prepaidLine?.itemCode, '模板无预付款项行')

        const afterAp = num(apLine?.currentBalance)
        const afterPrepaid = num(prepaidLine?.currentBalance)
        // 借记应付 6000：应付净值（应付−预付）应下降 6000
        expect((afterAp - afterPrepaid) - (beforeAp - beforePrepaid)).toBeCloseTo(-6_000, 0)
        await assertBalanceSheetLineMatchesConfig(
            request, ctx.headers, ctx.term, String(prepaidLine!.itemCode),
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })
})
