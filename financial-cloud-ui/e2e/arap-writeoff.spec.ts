import {expect, test} from '@playwright/test'
import {getCurrentTerm, loginViaApi} from './helpers/auth'

test.describe('arap writeoff L3', () => {
    test('open-items / confirm validation / aging method', async ({request}) => {
        let auth: Awaited<ReturnType<typeof loginViaApi>>
        try {
            auth = await loginViaApi(request)
        } catch {
            test.skip(true, 'login unavailable')
            return
        }
        const term = await getCurrentTerm(request, auth.headers, '')

        const bal = await request.get('/api/arap/balance', {
            headers: auth.headers,
            params: {side: 'AR', periodStart: term, periodEnd: term, includeZero: true},
        })
        const balBody = await bal.json()
        expect(balBody.code).toBe(0)

        const bad = await request.post('/api/arap/writeoff/confirm', {
            headers: auth.headers,
            data: {side: 'AR', counterpartId: '', legs: []},
        })
        const badBody = await bad.json()
        expect(badBody.code).not.toBe(0)

        const aging = await request.get('/api/arap/aging', {
            headers: auth.headers,
            params: {side: 'AR', asOfDate: `${term}-28`},
        })
        const ageBody = await aging.json()
        expect(ageBody.code).toBe(0)
        if ((ageBody.data || []).length) {
            expect(['OPEN_ITEM', 'FIFO_ESTIMATE']).toContain(ageBody.data[0].agingMethod)
        }

        const verify = await request.get('/api/settlement/verify', {headers: auth.headers})
        const vBody = await verify.json()
        const arap = (vBody.data || []).find((x: any) => String(x.item || '').includes('往来'))
        expect(arap).toBeTruthy()
        if (arap.warning) expect(arap.result).toBeTruthy()
    })
})
