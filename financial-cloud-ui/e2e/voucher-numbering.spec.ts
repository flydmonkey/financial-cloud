import {expect, test} from '@playwright/test'
import {getCurrentUser, loginViaApi} from './helpers/auth'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    fetchSuccessiveGaps,
    getVoucherDetail,
    submitVoucher,
    tryCreateDraftVoucher,
    tryDeleteVoucher,
} from './helpers/voucher'

/**
 * TC-VCH-011 ~ TC-VCH-013：凭证字号连号
 */
test.describe.serial('voucher numbering', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        firstWordNum: number
    } = {
        headers: {},
        bookId: '',
        firstWordNum: 0,
    }

    test('login and prepare', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
    })

    test('TC-VCH-011: word numbers auto increment', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')

        const p1 = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '连号-1', 11,
        )
        const id1 = await createDraftVoucher(request, ctx.headers, p1)
        const d1 = await getVoucherDetail(request, ctx.headers, id1)
        ctx.firstWordNum = Number(d1.wordNum)

        const p2 = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '连号-2', 12,
        )
        const id2 = await createDraftVoucher(request, ctx.headers, p2)
        const d2 = await getVoucherDetail(request, ctx.headers, id2)

        expect(Number(d2.wordNum)).toBe(ctx.firstWordNum + 1)
    })

    test('TC-VCH-012: duplicate word number is renumbered', async ({request}) => {
        test.skip(!ctx.bookId || !ctx.firstWordNum, '无前置字号')

        const duplicate = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '连号-重复', 13,
        )
        duplicate.wordNum = ctx.firstWordNum
        const result = await tryCreateDraftVoucher(request, ctx.headers, duplicate)

        expect(result.code).toBe(0)
        expect(result.message || '').toContain('凭证字号重复')

        const detail = await getVoucherDetail(request, ctx.headers, result.data as string)
        expect(Number(detail.wordNum)).toBeGreaterThan(ctx.firstWordNum)
    })

    test('TC-VCH-013: deleting middle draft causes successive gap after submit', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')

        const mk = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount,
            )
            return {
                payload,
                id: await createDraftVoucher(request, ctx.headers, payload),
            }
        }

        const v1 = await mk('断号-1', 14)
        const v2 = await mk('断号-2', 15)
        const v3 = await mk('断号-3', 16)

        const del = await tryDeleteVoucher(request, ctx.headers, v2.id)
        expect(del.code).toBe(0)

        const remain = await getVoucherDetail(request, ctx.headers, v3.id)
        expect(Number(remain.wordNum)).toBeGreaterThan(Number(v1.payload.wordNum))

        for (const voucher of [v1, v3]) {
            await submitVoucher(request, ctx.headers, voucher.payload, voucher.id)
            await auditVoucher(request, ctx.headers, voucher.id)
        }

        const gaps = await fetchSuccessiveGaps(request, ctx.headers)
        expect(gaps.length).toBeGreaterThan(0)
        test.info().annotations.push({
            type: 'note',
            description: `断号修复建议: ${gaps.map((g) => `${g.sourceWord}->${g.targetWord}`).join(', ')}`,
        })
    })
})
