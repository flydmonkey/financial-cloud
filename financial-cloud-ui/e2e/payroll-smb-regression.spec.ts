import {expect, test, type APIRequestContext} from '@playwright/test'
import {spawnSync} from 'node:child_process'
import {getCurrentUser, loginViaApi, type AuthSession} from './helpers/auth'
import {clearBooksViaScript, setupE2eBookViaApi} from './helpers/books'
import {getVoucherDetail, runVoucherToPosted, voucherDetailToPayload} from './helpers/voucher'
import {fetchSubjectBalances} from './helpers/reports'

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
        expect(process.env.FC_DB_NAME, 'payroll fixture requires an isolated database').toMatch(/^financial_cloud_e2e_/)
        clearBooksViaScript()
        const auth: AuthSession = await loginViaApi(request)
        const {headers} = auth
        // Independent first-month fixture. These amounts are test inputs, not policy defaults.
        await setupE2eBookViaApi(request, headers, {name: '工资完整闭环验收', enableDate: '2025-01'})
        const user = await getCurrentUser(request, headers)
        const bookId = user?.bookId
        expect(bookId, 'current user bookId').toBeTruthy()

        // 0) insurance defaults present
        const ins = await jsonGet(request, '/api/config/insurance_fund/getCurrent', headers)
        expect(ins.code).toBe(0)
        expect(Number(ins.data.payBase)).toBeGreaterThan(0)
        const config = {...ins.data, endowmentPersonal: 8, medicalPersonal: 2,
            unemploymentPersonal: 0.2, providentFundSupPersonal: 5,
            employmentInjuryPersonal: 0, maternityPersonal: 0, seriousMedicalPersonal: 0}
        const configured = await request.put('/api/config/insurance_fund/updateCurrent', {headers, data: config})
        expect((await configured.json()).code).toBe(0)

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
            payBasic: 4000,
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
        // 4800 × (8% + 2% + 0.2%) = 489.60; fund = 240; first-month tax = 0.
        const expected = {payAmount: 4000, insuranceEndowment: 384, insuranceMedical: 96,
            insuranceUnemployment: 9.6, totalSocialInsurance: 489.6, providentFund: 240,
            personalTax: 0, totalAmount: 3270.4}
        for (const [field, amount] of Object.entries(expected)) {
            expect(Number(tempRow[field]), `preview ${field}`).toBeCloseTo(amount, 2)
        }

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
        expect(salary, 'confirmed salary row').toBeTruthy()
        for (const [field, amount] of Object.entries(expected)) {
            expect(Number(salary[field]), `confirmed ${field}`).toBeCloseTo(amount, 2)
        }

        const belongDate = String(salary.belongDate || '').slice(0, 7)
        expect(belongDate).toMatch(/^\d{4}-\d{2}$/)

        const count = await jsonGet(request, `/api/employee/salary/count?belongDate=${belongDate}`, headers)
        expect(count.code).toBe(0)
        expect(Number(count.data)).toBeGreaterThan(0)

        const before = await fetchSubjectBalances(request, headers, belongDate)
        // 5) Both vouchers must succeed; a missing template is a failure.
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
        expect(payVoucher.code, payVoucher.message || 'payment voucher').toBe(0)
        expect(payVoucher.data).toBeTruthy()
        expect(payVoucher.data).not.toBe(accrual.data)
        const accrued = await getVoucherDetail(request, headers, accrual.data)
        const paid = await getVoucherDetail(request, headers, payVoucher.data)
        const sum = (voucher: typeof accrued, field: 'debitAmount' | 'creditAmount') =>
            voucher.items.reduce((total: number, item: any) => total + Number(item[field] || 0), 0)
        expect(sum(accrued, 'debitAmount')).toBe(4000)
        expect(sum(accrued, 'creditAmount')).toBe(4000)
        expect(sum(paid, 'debitAmount')).toBeCloseTo(3270.4, 2)
        expect(sum(paid, 'creditAmount')).toBeCloseTo(3270.4, 2)
        const posting = (value: typeof accrued, direction: 'debitAmount' | 'creditAmount', prefix: string) => {
            const rows = value.items.filter((item: any) => Number(item[direction] || 0) !== 0)
            expect(rows, `${prefix} ${direction} posting`).toHaveLength(1)
            expect(rows[0].subjectCode).toMatch(new RegExp(`^${prefix}(\\.|$)`))
            return rows[0].subjectCode as string
        }
        const expenseCode = posting(accrued, 'debitAmount', '5602')
        const payableCode = posting(accrued, 'creditAmount', '2211')
        expect(posting(paid, 'debitAmount', '2211')).toBe(payableCode)
        const bankCode = posting(paid, 'creditAmount', '1002')
        await runVoucherToPosted(request, headers, voucherDetailToPayload(accrued), accrual.data)
        await runVoucherToPosted(request, headers, voucherDetailToPayload(paid), payVoucher.data)
        const after = await fetchSubjectBalances(request, headers, belongDate)
        // Independent expected effects, rather than recomputing from generated vouchers.
        for (const [code, delta] of [[expenseCode, 4000], [payableCode, -729.6], [bankCode, -3270.4]] as const) {
            const initial = Number(before.find(row => row.subjectCode === code)?.balance || 0)
            const actual = after.find(row => row.subjectCode === code)
            expect(actual, `${code} balance row`).toBeTruthy()
            expect(Number(actual!.balance) - initial, `${code} posted balance delta`).toBeCloseTo(delta, 2)
        }
        for (const voucherType of [2, 3]) {
            const repeated = await jsonPost(request, '/api/employee/salary/generate-voucher', headers,
                {id: salary.id, bookId, voucherType})
            expect(repeated.code, 'repeat generation must fail').not.toBe(0)
        }
        expect(await fetchSubjectBalances(request, headers, belongDate)).toEqual(after)

        // 6) export bank payment file
        const exportRes = await request.get(
            `/api/employee/salary/export-payment?belongDate=${belongDate}`,
            {headers},
        )
        expect(exportRes.ok(), `export-payment HTTP ${exportRes.status()}`).toBeTruthy()
        const ctype = exportRes.headers()['content-type'] || ''
        expect(ctype, 'must export a workbook rather than a JSON response').not.toContain('json')
        const buf = await exportRes.body()
        expect(buf.subarray(0, 4).toString('hex'), 'valid XLSX archive').toBe('504b0304')
        const parsed = spawnSync(process.platform === 'win32' ? 'python' : 'python3', ['-X', 'utf8', '-c', `
import io,json,sys,zipfile,xml.etree.ElementTree as ET
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(io.BytesIO(sys.stdin.buffer.read())) as z:
    shared=[''.join(n.itertext()) for n in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('s:si',ns)]
    rows=[]
    for row in ET.fromstring(z.read('xl/worksheets/sheet1.xml')).findall('.//s:row',ns):
        cells=[]
        for c in row.findall('s:c',ns):
            value=c.find('s:v',ns)
            text=value.text if value is not None else ''
            cells.append(shared[int(text)] if c.get('t')=='s' else text)
        rows.append(cells)
    print(json.dumps(rows,ensure_ascii=False))
`], {input: buf, encoding: 'utf8'})
        expect(parsed.status, parsed.stderr).toBe(0)
        const rows = JSON.parse(parsed.stdout) as string[][]
        const payment = rows.find(row => row[0] === empBody.employeeNumber)
        expect(payment, 'export contains this employee').toBeTruthy()
        expect(payment!.slice(0, 4)).toEqual([empBody.employeeNumber, empBody.displayName, empBody.bankName, empBody.bankCardNo])
        expect(Number(payment![4]), 'export net pay').toBeCloseTo(3270.4, 2)
        expect(payment![5]).toBe('2025-01')
    })
})
