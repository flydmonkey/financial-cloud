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

async function jsonWrite(request: APIRequestContext, url: string, headers: Record<string, string>, data?: unknown,
                         method: 'post' | 'put' | 'delete' = 'post') {
    const res = await request[method](url, {headers, data})
    expect(res.ok(), `${url} HTTP ${res.status()}`).toBeTruthy()
    return res.json()
}

function expectRejected(body: any, label: string) {
    expect(typeof body.code, `${label}: ${JSON.stringify(body)}`).toBe('number')
    expect(body.code, `${label}: ${body.message}`).not.toBe(0)
}

// Removing a draft may update audit metadata and one link; the salary itself must stay identical.
function salaryContent(salary: any) {
    const content = {...salary}
    for (const field of ['accrualVoucherId', 'salaryVoucherId', 'modifiedBy', 'modifiedDate']) delete content[field]
    return content
}

test.describe('payroll SMB min-loop regression', () => {
    test('employee custom base → preview → push → draft correction → posted integrity → payment export', async ({request}) => {
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
            const org = await jsonWrite(request, '/api/orgs/add', headers, {
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
        const saveEmp = await jsonWrite(request, '/api/salary/employee/save', headers, empBody)
        expect(saveEmp.code, saveEmp.message || JSON.stringify(saveEmp)).toBe(0)
        // Same fixed inputs, separate employee: provides an unlinked row for an atomic batch-delete check.
        const unlinkedEmpBody = {...empBody, displayName: '混合删除回归员',
            employeeNumber: `${empBody.employeeNumber}B`, idCardNo: `1101011991${String(Date.now()).slice(-8)}`}
        const saveUnlinkedEmp = await jsonWrite(request, '/api/salary/employee/save', headers, unlinkedEmpBody)
        expect(saveUnlinkedEmp.code, saveUnlinkedEmp.message || 'create unlinked employee').toBe(0)

        const empPage = await jsonGet(request, '/api/salary/employee/fetch?pageNumber=1&pageSize=50', headers)
        expect(empPage.code).toBe(0)
        const employee = (empPage.data?.records || []).find((e: any) => e.employeeNumber === empBody.employeeNumber)
        expect(employee, 'created employee visible').toBeTruthy()
        const unlinkedEmployee = (empPage.data?.records || []).find((e: any) => e.employeeNumber === unlinkedEmpBody.employeeNumber)
        expect(unlinkedEmployee, 'unlinked employee visible').toBeTruthy()

        // 3) generate salary preview (temp)
        const preview = await jsonWrite(request, '/api/salary/detail/createTable', headers, {bookId})
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
        const push = await jsonWrite(request, '/api/salary/detail/submit-detail', headers, {})
        expect(push.code, push.message || 'submit-detail').toBe(0)

        const salaryPage = await jsonGet(
            request,
            '/api/employee/salary/fetch?pageNumber=1&pageSize=50',
            headers,
        )
        expect(salaryPage.code).toBe(0)
        const salary = (salaryPage.data?.records || []).find((r: any) => r.employeeId === employee.id)
        expect(salary, 'confirmed salary row').toBeTruthy()
        const unlinkedSalary = (salaryPage.data?.records || []).find((r: any) => r.employeeId === unlinkedEmployee.id)
        expect(unlinkedSalary, 'unlinked confirmed salary row').toBeTruthy()
        expect(unlinkedSalary.belongDate).toBe(salary.belongDate)
        expect(unlinkedSalary.accrualVoucherId).toBeNull()
        expect(unlinkedSalary.salaryVoucherId).toBeNull()
        for (const [field, amount] of Object.entries(expected)) {
            expect(Number(salary[field]), `confirmed ${field}`).toBeCloseTo(amount, 2)
        }

        const belongDate = String(salary.belongDate || '').slice(0, 7)
        expect(belongDate).toMatch(/^\d{4}-\d{2}$/)

        const count = await jsonGet(request, `/api/employee/salary/count?belongDate=${belongDate}`, headers)
        expect(count.code).toBe(0)
        expect(Number(count.data)).toBe(2)

        const readSalary = async (id = salary.id) => {
            const result = await jsonGet(request, `/api/employee/salary/get/${id}`, headers)
            expect(result.code, result.message || 'read confirmed salary').toBe(0)
            expect(result.data, 'salary detail exists').toBeTruthy()
            return result.data
        }

        const before = await fetchSubjectBalances(request, headers, belongDate)
        // 5) Both vouchers must succeed; a missing template is a failure.
        const accrual = await jsonWrite(request, '/api/employee/salary/generate-voucher', headers, {
            id: salary.id,
            bookId,
            voucherType: 2,
        })
        expect(accrual.code, accrual.message || 'accrual voucher').toBe(0)
        expect(accrual.data).toBeTruthy()

        const payVoucher = await jsonWrite(request, '/api/employee/salary/generate-voucher', headers, {
            id: salary.id,
            bookId,
            voucherType: 3,
        })
        expect(payVoucher.code, payVoucher.message || 'payment voucher').toBe(0)
        expect(payVoucher.data).toBeTruthy()
        expect(payVoucher.data).not.toBe(accrual.data)
        let accrualVoucherId = accrual.data as string
        let paymentVoucherId = payVoucher.data as string
        const linkedSalary = await readSalary()
        expect(linkedSalary.accrualVoucherId).toBe(accrualVoucherId)
        expect(linkedSalary.salaryVoucherId).toBe(paymentVoucherId)
        // Both draft removal paths clear only their selected link and allow a new voucher afterward.
        for (const voucherType of [2, 3]) {
            const link = voucherType === 2 ? 'accrualVoucherId' : 'salaryVoucherId'
            const otherLink = voucherType === 2 ? 'salaryVoucherId' : 'accrualVoucherId'
            const current = await readSalary()
            const removedId = current[link] as string
            const otherVoucher = await getVoucherDetail(request, headers, current[otherLink])
            expect((await getVoucherDetail(request, headers, removedId)).status).toBe('draft')
            const removed = await jsonWrite(request, '/api/employee/salary/delete-voucher', headers,
                {id: salary.id, bookId, voucherType})
            expect(removed.code, removed.message || 'remove draft payroll voucher').toBe(0)
            const corrected = await readSalary()
            expect(corrected[link], 'only selected link cleared').toBeNull()
            expect(corrected[otherLink], 'other link preserved').toBe(current[otherLink])
            expect(salaryContent(corrected), 'draft removal preserves salary values').toEqual(salaryContent(current))
            const missing = await jsonGet(request, `/api/voucher/get/${removedId}`, headers)
            expectRejected(missing, 'removed draft no longer exists')
            expect(missing.data).toBeNull()
            expect(await getVoucherDetail(request, headers, current[otherLink]), 'other voucher unchanged').toEqual(otherVoucher)
            expect(await fetchSubjectBalances(request, headers, belongDate), 'draft removal preserves balances').toEqual(before)

            const regenerated = await jsonWrite(request, '/api/employee/salary/generate-voucher', headers,
                {id: salary.id, bookId, voucherType})
            expect(regenerated.code, regenerated.message || 'regenerate after draft removal').toBe(0)
            expect(regenerated.data).toEqual(expect.any(String))
            expect(regenerated.data).not.toBe(removedId)
            expect(regenerated.data).not.toBe(current[otherLink])
            const relinked = await readSalary()
            expect(relinked[link]).toBe(regenerated.data)
            expect(relinked[otherLink]).toBe(current[otherLink])
            expect(salaryContent(relinked), 'regeneration preserves salary values').toEqual(salaryContent(current))
            expect(await getVoucherDetail(request, headers, current[otherLink]), 'regeneration preserves other voucher').toEqual(otherVoucher)
            expect(await fetchSubjectBalances(request, headers, belongDate), 'draft regeneration preserves balances').toEqual(before)
            if (voucherType === 2) accrualVoucherId = regenerated.data
            else paymentVoucherId = regenerated.data
        }
        const accrued = await getVoucherDetail(request, headers, accrualVoucherId)
        const paid = await getVoucherDetail(request, headers, paymentVoucherId)
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
        await runVoucherToPosted(request, headers, voucherDetailToPayload(accrued), accrualVoucherId)
        await runVoucherToPosted(request, headers, voucherDetailToPayload(paid), paymentVoucherId)
        const after = await fetchSubjectBalances(request, headers, belongDate)
        // Independent expected effects, rather than recomputing from generated vouchers.
        for (const [code, delta] of [[expenseCode, 4000], [payableCode, -729.6], [bankCode, -3270.4]] as const) {
            const initial = Number(before.find(row => row.subjectCode === code)?.balance || 0)
            const actual = after.find(row => row.subjectCode === code)
            expect(actual, `${code} balance row`).toBeTruthy()
            expect(Number(actual!.balance) - initial, `${code} posted balance delta`).toBeCloseTo(delta, 2)
        }
        const postedSalary = await readSalary()
        const originalUnlinkedSalary = await readSalary(unlinkedSalary.id)
        const postedAccrued = await getVoucherDetail(request, headers, accrualVoucherId)
        const postedPaid = await getVoucherDetail(request, headers, paymentVoucherId)
        expect(postedSalary.accrualVoucherId).toBe(accrualVoucherId)
        expect(postedSalary.salaryVoucherId).toBe(paymentVoucherId)
        expect(salaryContent(postedSalary)).toEqual(salaryContent(linkedSalary))
        for (const voucher of [postedAccrued, postedPaid]) {
            expect(voucher.status).toBe('completed')
            expect(voucher.senderId, 'voucher was actually posted').toBeTruthy()
        }
        const assertPostedUnchanged = async () => {
            expect(await readSalary(), 'posted salary amounts and both links unchanged').toEqual(postedSalary)
            expect(await readSalary(unlinkedSalary.id), 'unlinked row survives rejected batch').toEqual(originalUnlinkedSalary)
            expect(await getVoucherDetail(request, headers, accrualVoucherId), 'posted accrual state and entries unchanged').toEqual(postedAccrued)
            expect(await getVoucherDetail(request, headers, paymentVoucherId), 'posted payment state and entries unchanged').toEqual(postedPaid)
            expect(await fetchSubjectBalances(request, headers, belongDate), 'all posted balances unchanged').toEqual(after)
        }
        const edited = await jsonWrite(request, '/api/employee/salary/update', headers,
            {id: salary.id, bookId, payBasic: 9999, payAmount: 9999, totalAmount: 9999}, 'put')
        expectRejected(edited, 'editing linked payroll must fail')
        expect(edited.message, 'linked edit explains required correction').toMatch(/凭证/)
        await assertPostedUnchanged()

        for (const listIds of [[salary.id], [unlinkedSalary.id, salary.id]]) {
            const removed = await jsonWrite(request, '/api/employee/salary/delete', headers, {listIds}, 'delete')
            expectRejected(removed, 'deleting linked payroll or a mixed batch must fail')
            expect(removed.message, 'linked deletion explains required correction').toMatch(/凭证/)
            await assertPostedUnchanged()
        }
        for (const voucherType of [2, 3]) {
            const removed = await jsonWrite(request, '/api/employee/salary/delete-voucher', headers,
                {id: salary.id, bookId, voucherType})
            expectRejected(removed, 'removing posted payroll voucher must fail')
            expect(removed.message, 'original voucher status rejection is returned').toMatch(/凭证.*删除|删除.*凭证/)
            await assertPostedUnchanged()
        }
        for (const voucherType of [2, 3]) {
            const repeated = await jsonWrite(request, '/api/employee/salary/generate-voucher', headers,
                {id: salary.id, bookId, voucherType})
            expectRejected(repeated, 'repeat generation remains blocked after rejected mutations')
            await assertPostedUnchanged()
        }
        // The unlinked member can still be deleted normally after the rejected mixed batch.
        const removedUnlinked = await jsonWrite(request, '/api/employee/salary/delete', headers,
            {listIds: [unlinkedSalary.id]}, 'delete')
        expect(removedUnlinked.code, removedUnlinked.message || 'delete unlinked payroll detail').toBe(0)
        const remainingPage = await jsonGet(request, '/api/employee/salary/fetch?pageNumber=1&pageSize=50', headers)
        expect(remainingPage.code).toBe(0)
        expect(remainingPage.data.records.map((row: any) => row.id)).toEqual([salary.id])
        expect(await readSalary()).toEqual(postedSalary)
        expect(await getVoucherDetail(request, headers, accrualVoucherId)).toEqual(postedAccrued)
        expect(await getVoucherDetail(request, headers, paymentVoucherId)).toEqual(postedPaid)
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
