import {expect, test, type APIRequestContext} from '@playwright/test'
import {assertReportsBalanced, assertReportsReconciled} from './helpers/reports'

function fixture(difference: number, options: {fabricatedTotal?: boolean; fabricatedMetadata?: boolean; costExpense?: boolean} = {}) {
    const liabilityTotal = options.fabricatedTotal ? 100_000 : 100_000 - difference
    const paths: string[] = []
    const context = {
        get: async (path: string) => {
            paths.push(path)
            let data: unknown
            if (path.startsWith('/api/booksubject/fetch')) {
                const page = new URL(path, 'http://test').searchParams.get('pageNum')
                data = {records: page === '1' ? [{code: '5001'}, {code: '500101'}] : [{code: '5602'}], total: 501}
            } else if (path.startsWith('/api/statement/subject-balance')) {
                data = [
                    // Parent total duplicates the child and must not be added again.
                    {sourceId: 'revenue', subjectCode: '5001', closingBalanceCredit: Math.max(difference, 0)},
                    {sourceId: 'revenue-child', parentId: 'revenue', subjectCode: '500101', closingBalanceCredit: Math.max(difference, 0)},
                    {sourceId: 'expense', subjectCode: options.costExpense ? '540101' : '5602', closingBalanceDebit: Math.max(-difference, 0)},
                    {sourceId: 'cash', subjectCode: '1002', closingBalanceDebit: 100_000},
                ]
            } else {
                data = {
                    bookId: 'test-book',
                    items: {
                        assets: [{itemCode: '1199', itemName: '资产总计', currentBalance: 100_000}],
                        liability: [{itemCode: '2299', itemName: '负债及权益总计', currentBalance: liabilityTotal}],
                        assetTotal: 100_000,
                        liabilityTotal,
                        balanced: Math.abs(difference) <= 0.01,
                        balanceDifference: options.fabricatedMetadata ? 0 : difference,
                    },
                }
            }
            return {ok: () => true, json: async () => ({code: 0, data})}
        },
    } as unknown as APIRequestContext
    return {context, paths}
}

for (const difference of [10_000, -10_000, 0]) {
    test(`actual difference ${difference} reconciles to leaf P&L balances`, async () => {
        const {context, paths} = fixture(difference)
        await assertReportsReconciled(context, {}, '2026-08')
        expect(paths.some((path) => path.includes('category=6') && path.includes('pageNum=2'))).toBe(true)
    })
}

test('small-enterprise cost-class expenses reconcile even outside category 6', async () => {
    const {context} = fixture(-10_000, {costExpense: true})
    await assertReportsReconciled(context, {}, '2026-08')
})

test('fabricated equality cannot hide remaining P&L balances', async () => {
    const {context} = fixture(10_000, {fabricatedTotal: true})
    await expect(assertReportsReconciled(context, {}, '2026-08')).rejects.toThrow('真实报表差额')
})

test('incorrect response metadata is rejected', async () => {
    const {context} = fixture(-10_000, {fabricatedMetadata: true})
    await expect(assertReportsReconciled(context, {}, '2026-08')).rejects.toThrow()
})

test('post-carry strict equality remains required', async () => {
    const {context} = fixture(10_000)
    await expect(assertReportsBalanced(context, {}, '2026-08')).rejects.toThrow()
})
