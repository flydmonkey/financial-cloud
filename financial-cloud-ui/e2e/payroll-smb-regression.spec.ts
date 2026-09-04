import {expect, test, type APIRequestContext} from '@playwright/test'
import {getCurrentUser, loginViaApi, type AuthSession} from './helpers/auth'

async function jsonGet(request: APIRequestContext, url: string, headers: Record<string, string>) {
    const res = await request.get(url, {headers})
    expect(res.ok(), `${url} HTTP ${res.status()}`).toBeTruthy()
    return res.json()
}

async function jsonPost(request: APIRequestContext, url: string, headers: Record<string, string>, data?: unknown) {
    const res = await request.post(url, {headers, data})
    expect(res.ok(), `${url} HTTP ${res.status()}`).toBeTruthy()
    return res.json()
}

test.describe('payroll SMB min-loop regression', () => {
    test('employee custom base → preview → push → voucher → payment export', async ({request}) => {
        test.setTimeout(120_000)
        const auth: AuthSession = await loginViaApi(request)
        const {headers} = auth
        const user = await getCurrentUser(request, headers)
        const bookId = user?.bookId
        expect(bookId, 'current user bookId').toBeTruthy()

        // 0) insurance defaults present
        const ins = await jsonGet(request, '/api/config/insurance_fund/getCurrent', headers)
        expect(ins.code).toBe(0)
        expect(Number(ins.data.payBase)).toBeGreaterThan(0)

        // 1) ensure department
        const orgList = await jsonGet(request, '/api/orgs/fetch?pageNumber=1&pageSize=20', headers)
        expect(orgList.code).toBe(0)
        let departmentId = orgList.data?.records?.[0]?.id as string | undefined
        if (!departmentId) {
            const org = await jsonPost(request, '/api/orgs/add', headers, {
                orgCode: `D-PAY-${Date.now().toString().slice(-6)}`,
                orgName: '薪资回归部门',
                fullName: '薪资回归部门',
                type: 'department',
                parentId: null,
                status: 1,
                level: 1,
                sortIndex: 1,
            })
            expect(org.code, org.message || 'create org').toBe(0)
            departmentId = org.data?.id
        }
        expect(departmentId).toBeTruthy()

        // 2) create NORMAL employee with custom SI base + bank card
        const customBase = 4800
        const empBody = {
            displayName: '薪资回归员',
            employeeNumber: `PR${Date.now().toString().slice(-8)}`,
            gender: 1,
            idType: 1,
            idCardNo: `1101011990${String(Date.now()).slice(-8)}`,
            employeeType: 'NORMAL',
            employeeStatus: 'RESIDENT',
            departmentId,
            status: 1,
            payBasic: 8000,
            payMerit: 0,
            payPost: 0,
            laborFee: 0,
            payBaseRule: 1,
            payBaseNumber: customBase,
            bankName: '测试银行',
            bankCardNo: '6222021234567890123',
        }
        const saveEmp = await jsonPost(request, '/api/salary/employee/save', headers, empBody)
        expect(saveEmp.code, saveEmp.message || JSON.stringify(saveEmp)).toBe(0)

        const empPage = await jsonGet(request, '/api/salary/employee/fetch?pageNumber=1&pageSize=50', headers)
        expect(empPage.code).toBe(0)
        const employee = (empPage.data?.records || []).find((e: any) => e.employeeNumber === empBody.employeeNumber)
        expect(employee, 'created employee visible').toBeTruthy()

        // 3) generate salary preview (temp)
        const preview = await jsonPost(request, '/api/salary/detail/createTable', headers, {bookId})
        expect(preview.code, preview.message || 'createTable').toBe(0)

        const tempPage = await jsonGet(request, '/api/salary/detail/fetch?pageNumber=1&pageSize=50', headers)
        expect(tempPage.code).toBe(0)
        const tempRow = (tempPage.data?.records || []).find((r: any) => r.employeeId === employee.id)
        expect(tempRow, 'preview row for employee').toBeTruthy()
        expect(Number(tempRow.effectivePayBase)).toBe(customBase)
        expect(tempRow.payBaseSource).toBeTruthy()

        // 4) push confirmed salary detail
        const push = await jsonPost(request, '/api/salary/detail/submit-detail', headers, {})
        expect(push.code, push.message || 'submit-detail').toBe(0)

        const salaryPage = await jsonGet(
            request,
            `/api/employee/salary/fetch?pageNumber=1&pageSize=50&employeeId=${employee.id}`,
            headers,
        )
        expect(salaryPage.code).toBe(0)
        const salary = (salaryPage.data?.records || []).find((r: any) => r.employeeId === employee.id)
            || salaryPage.data?.records?.[0]
        expect(salary, 'confirmed salary row').toBeTruthy()

        const belongDate = String(salary.belongDate || '').slice(0, 7)
        expect(belongDate).toMatch(/^\d{4}-\d{2}$/)

        const count = await jsonGet(request, `/api/employee/salary/count?belongDate=${belongDate}`, headers)
        expect(count.code).toBe(0)
        expect(Number(count.data)).toBeGreaterThan(0)

        // 5) generate vouchers — NORMAL accrual uses jt_gz; payment may lack zf_gz
        const accrual = await jsonPost(request, '/api/employee/salary/generate-voucher', headers, {
            id: salary.id,
            bookId,
            voucherType: 2,
        })
        expect(accrual.code, accrual.message || 'accrual voucher').toBe(0)
        expect(accrual.data).toBeTruthy()

        const payVoucher = await jsonPost(request, '/api/employee/salary/generate-voucher', headers, {
            id: salary.id,
            bookId,
            voucherType: 3,
        })
        if (payVoucher.code !== 0) {
            expect(String(payVoucher.message || '')).toMatch(/凭证模板\[zf_gz\]未设置/)
        }

        // 6) export bank payment file
        const exportRes = await request.get(
            `/api/employee/salary/export-payment?belongDate=${belongDate}`,
            {headers},
        )
        expect(exportRes.ok(), `export-payment HTTP ${exportRes.status()}`).toBeTruthy()
        const ctype = exportRes.headers()['content-type'] || ''
        if (ctype.includes('json')) {
            const body = await exportRes.json()
            expect(body.code, body.message || 'export-payment').toBe(0)
        } else {
            const buf = await exportRes.body()
            expect(buf.byteLength).toBeGreaterThan(0)
        }
    })
})
