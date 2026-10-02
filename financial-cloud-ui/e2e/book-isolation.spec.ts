import {expect, test, type APIRequestContext} from '@playwright/test'
import {execFileSync} from 'node:child_process'
import {getCurrentUser, getCurrentTerm, loginViaApi, loginViaApiAs} from './helpers/auth'
import {buildBalancedVoucherPayload, createDraftVoucher} from './helpers/voucher'

// These tests intentionally seed fake data. The Python helper refuses non-isolated databases.
test.describe.serial('全模块账套隔离', () => {
    let headers: Record<string,string>, viewer: Record<string,string>
    let bookA: string, bookB: string, term: string, viewerId: string
    let own: Record<string,string>, foreign: Record<string,string>
    let baseline: any
    let viewerToken: string
    const fixture = (mode: string, book: string, ...args: string[]) => JSON.parse(execFileSync(
        'python', ['tools/book_isolation_fixture.py', mode, book, ...args], {cwd:'..', encoding:'utf8'}))
    const denied = async (request: APIRequestContext, method: 'get'|'post'|'put'|'delete', url: string, data?: any, auth = headers) => {
        const response = await request[method](url, {headers:auth, ...(data === undefined ? {} : {data})})
        const body = await response.json()
        expect(body.code, `${method} ${url}: ${JSON.stringify(body)}`).toBe(500014)
    }
    test.beforeAll(async ({request}) => {
        headers = (await loginViaApi(request)).headers
        bookA = (await getCurrentUser(request, headers)).bookId
        term = await getCurrentTerm(request, headers, bookA)
        const name = `隔离回归-${Date.now()}`
        const created = await (await request.post('/api/book/save', {headers, data:{name, companyName:name, standardId:'1', enableDate:term, vatType:1, voucherReviewed:1, status:1}})).json()
        expect(created.code, created.message).toBe(0)
        const books = await (await request.get('/api/book/fetchAll',{headers})).json()
        bookB = books.data.find((row:any) => row.name === name).id
        own = fixture('seed',bookA,term,`own-${Date.now()}`)
        foreign = fixture('seed',bookB,term,`foreign-${Date.now()}`)
        expect((await (await request.get(`/api/users/switchBook/${bookB}`, {headers})).json()).code).toBe(0)
        foreign.voucher = await createDraftVoucher(request, headers, await buildBalancedVoucherPayload(request,headers,bookB,'B 隔离回归'))
        expect((await (await request.get(`/api/users/switchBook/${bookA}`, {headers})).json()).code).toBe(0)
        own.voucher = await createDraftVoucher(request, headers, await buildBalancedVoucherPayload(request,headers,bookA,'A 隔离回归'))
        const username = `isolation_viewer_${Date.now()}`, password = 'Audit@2026!'
        const made = await (await request.post('/api/users/add',{headers,data:{username,password,displayName:'隔离只读用户',userType:'EMPLOYEE',userState:'RESIDENT',status:1,sortIndex:99}})).json()
        expect(made.code,made.message).toBe(0)
        viewerId = (await (await request.get(`/api/users/getByUsername/${username}`,{headers})).json()).data.id
        expect((await (await request.post('/api/book/members/grant',{headers,data:{bookId:bookA,userId:viewerId,roleId:'ROLE_VIEWER'}})).json()).code).toBe(0)
        const viewerSession = await loginViaApiAs(request,username,password)
        viewer = viewerSession.headers
        viewerToken = viewerSession.token
        baseline = fixture('snapshot',bookB)
    })
    test.afterEach(() => expect(fixture('snapshot',bookB)).toEqual(baseline))
    test('只读用户不能切换未授权账套或通过旧入口自行提权', async ({request}) => {
        await denied(request,'get',`/api/users/switchBook/${bookB}`,undefined,viewer)
        expect((await getCurrentUser(request,viewer)).bookId).toBe(bookA)
        await denied(request,'post','/api/permissions/permissionBook/add',{userId:viewerId,bookIds:[bookB],roleId:'ROLE_ADMINISTRATORS'},viewer)
        await denied(request,'post','/api/idm/groupmembers/add',{roleId:'ROLE_ADMINISTRATORS',memberIds:[viewerId],type:'USER'},viewer)
        await denied(request,'get',`/api/book/get/${bookB}`,undefined,viewer)
        await denied(request,'get',`/api/book/members/list?bookId=${bookB}`,undefined,viewer)
        const books = await (await request.get('/api/book/fetchAll',{headers:viewer})).json()
        expect(books.data.map((row:any)=>row.id)).toEqual([bookA])
    })
    test('原四模块及新增模块拒绝外账套明细', async ({request}) => {
        for (const [url,id] of [
            ['/api/voucher/get/',foreign.voucher], ['/api/fixed-asset/card/get/',foreign.asset],
            ['/api/fixed-asset/category/get/',foreign.category], ['/api/employee/salary/get/',foreign.salary],
            ['/api/salary/detail/get/',foreign.salaryTemp], ['/api/employee/taxdeduction/get/',foreign.taxDeduction],
            ['/api/salary/employee/get/',foreign.employee], ['/api/journal/account/get/',foreign.account],
            ['/api/journal/entry/get/',foreign.entry], ['/api/base/assist-acc/get/',foreign.assist],
            ['/api/orgs/get/',foreign.org], ['/api/config/sys/get/',foreign.config],
            ['/api/config/salary/formula/get/',foreign.formula],
        ]) await denied(request,'get',url+id)
        await denied(request,'get',`/api/orgs/get/${foreign.org}`,undefined,viewer)
    })
    test('科目拒绝伪造账套参数，缺省参数只查询当前账套', async ({request}) => {
        for (const url of [`/api/booksubject/tree/${bookB}`,`/api/booksubject/fetch?bookId=${bookB}`,`/api/booksubject/get?bookId=${bookB}&id=${foreign.subject}`]) await denied(request,'get',url)
        const rows = await (await request.get('/api/booksubject/fetch?pageSize=500',{headers})).json()
        expect(rows.code).toBe(0)
        expect(rows.data.records.length).toBeGreaterThan(0)
        expect(rows.data.records.every((row:any)=>row.bookId===bookA)).toBe(true)
    })
    test('外账套修改、删除和账户关联均被拒绝', async ({request}) => {
        await denied(request,'put','/api/employee/salary/update',{id:foreign.salary,payBasic:4321})
        await denied(request,'put','/api/journal/account/update',{id:foreign.account,accName:'越界'})
        await denied(request,'post','/api/journal/entry/add',{accId:foreign.account,direction:'i',income:7,tradeDate:`${term}-15 12:00:00`})
        await denied(request,'delete','/api/fixed-asset/card/delete',{listIds:[foreign.asset]})
        await denied(request,'put','/api/voucher/update',{...await buildBalancedVoucherPayload(request,headers,bookA,'越界'),id:foreign.voucher,word:'记-1'})
    })
    test('混合批量请求整笔拒绝，A 和 B 均不改变', async ({request}) => {
        const before = fixture('snapshot',bookA)
        await denied(request,'delete','/api/fixed-asset/card/delete',{listIds:[own.asset,foreign.asset]})
        await denied(request,'post',`/api/voucher/submit/${own.voucher},${foreign.voucher}`)
        expect(fixture('snapshot',bookA)).toEqual(before)
    })
    test('期初及报表配置不能修改或搬迁外账套记录', async ({request}) => {
        await denied(request,'post','/api/config/cash-flow-balance/save',{cashFlowItemDtos:[{id:foreign.cashFlow,balance:77}]})
        await denied(request,'post','/api/statement/config/balance-sheet',{id:foreign.balanceSheet,itemName:'越界'})
        await denied(request,'post','/api/statement/config/income',{id:foreign.income,itemName:'越界'})
        await denied(request,'put','/api/config/insurance_fund/updateCurrent',{id:foreign.insurance})
        await denied(request,'put','/api/config/salary/formula/update',{id:foreign.formula,ruleName:'越界公式'})
        await denied(request,'delete','/api/config/salary/formula/delete',{listIds:[foreign.formula]})
        const formulas = await (await request.get('/api/config/salary/formula/fetch?pageSize=500',{headers})).json()
        expect(formulas.data.records.every((row:any)=>row.bookId===bookA)).toBe(true)
        await denied(request,'post','/api/statement/config/rules/001',[{id:foreign.rule,subjectCode:'1001',rule:'debit',symbol:'+'}])
    })
    test('关联资产、科目及嵌套凭证明细检查归属', async ({request}) => {
        await denied(request,'put',`/api/fixed-asset/depreciation/work?yearPeriod=${term}`,[{assetId:foreign.asset,workload:1}])
        const payload = await buildBalancedVoucherPayload(request,headers,bookA,'关联越界')
        payload.items[0].subjectId = foreign.subject
        await denied(request,'post','/api/voucher/draft',payload)
    })
    test('存量流水关联外账套账户时，读取和修改也被拒绝', async ({request}) => {
        fixture('link-entry',bookA,own.entry,foreign.account)
        try {
            await denied(request,'get',`/api/journal/entry/get/${own.entry}`)
            await denied(request,'put','/api/journal/entry/update',{id:own.entry,income:7})
        } finally { fixture('link-entry',bookA,own.entry,own.account) }
    })
    test('同账套工资、账户和期初配置可正常修改', async ({request}) => {
        for (const [url,method,data] of [
            ['/api/employee/salary/update','put',{id:own.salary,payBasic:2000}],
            ['/api/journal/account/update','put',{id:own.account,accName:'A正常修改'}],
            ['/api/config/cash-flow-balance/save','post',{cashFlowItemDtos:[{id:own.cashFlow,balance:55}]}],
            ['/api/config/salary/formula/update','put',{id:own.formula,ruleName:'A正常工资公式'}],
        ] as const) {
            const body = await (await request[method](url,{headers,data})).json()
            expect(body.code,body.message).toBe(0)
        }
    })
    test('模板缺省列表不混入 B，外账套详情及删除被拒绝', async ({request}) => {
        const body = await (await request.get('/api/vouchertemplate/fetch?pageSize=500',{headers})).json()
        expect(body.code).toBe(0)
        expect(body.data.records.length).toBeGreaterThan(0)
        expect(body.data.records.every((row:any)=>row.relatedId===bookA)).toBe(true)
        await denied(request,'get',`/api/vouchertemplate/get?id=${foreign.template}`)
        await denied(request,'delete','/api/vouchertemplate/delete',{listIds:[foreign.template]})
        const standard = await (await request.get('/api/vouchertemplate/fetch?relatedId=1&pageSize=500',{headers:viewer})).json()
        expect(standard.code).toBe(0)
        expect(standard.data.records.length).toBeGreaterThan(0)
        expect(standard.data.records.every((row:any)=>row.relatedId==='1')).toBe(true)
    })
    test('通用文件入口不能绕过业务附件归属或删除关联文件', async ({request}) => {
        await denied(request,'get',`/api/filestorage/image/${foreign.file}`)
        await denied(request,'get',`/api/filestorage/image/${foreign.file}`,undefined,viewer)
        await denied(request,'get',`/api/filestorage/image/getByIds?ids=${own.file},${foreign.file}`)
        await denied(request,'delete',`/api/filestorage/image/delete?ids=${own.file},${foreign.file}`)
        expect((await (await request.get(`/api/filestorage/image/${own.file}`,{headers})).json()).code).toBe(0)
    })
    test('合法切换和正常明细继续可用', async ({request}) => {
        expect((await (await request.get(`/api/users/switchBook/${bookB}`,{headers})).json()).code).toBe(0)
        try {
            for (const url of [`/api/journal/account/get/${foreign.account}`,`/api/orgs/get/${foreign.org}`,`/api/voucher/get/${foreign.voucher}`]) {
                const body = await (await request.get(url,{headers})).json()
                expect(body.code,body.message).toBe(0)
                expect(body.data.bookId).toBe(bookB)
            }
        } finally { await request.get(`/api/users/switchBook/${bookA}`,{headers}) }
    })
    test('同一用户的两个会话保持各自的当前账套', async ({request}) => {
        const other = (await loginViaApi(request)).headers
        expect((await (await request.get(`/api/users/switchBook/${bookB}`,{headers:other})).json()).code).toBe(0)
        try {
            expect((await getCurrentUser(request,headers)).bookId).toBe(bookA)
            expect((await getCurrentUser(request,other)).bookId).toBe(bookB)
            expect((await (await request.get(`/api/journal/account/get/${own.account}`,{headers})).json()).data.bookId).toBe(bookA)
            await denied(request,'get',`/api/journal/account/get/${foreign.account}`)
        } finally { await request.get(`/api/users/switchBook/${bookA}`,{headers:other}) }
    })
    test('全局产品角色不能通过修改定义改变其他账套权限', async ({request}) => {
        const role = await (await request.get('/api/idm/groups/get/ROLE_VIEWER',{headers})).json()
        expect(role.code).toBe(0)
        await denied(request,'put','/api/idm/groups/update',{...role.data,roleCode:'ROLE_ADMINISTRATORS'})
        await denied(request,'delete','/api/idm/groups/delete?ids=ROLE_VIEWER')
        await denied(request,'put','/api/idm/groups/update',role.data,viewer)
    })
    test('只读用户不能修改本账套业务和工资公式或全局税率', async ({request}) => {
        const before = fixture('snapshot',bookA)
        await denied(request,'put','/api/employee/salary/update',{id:own.salary,payBasic:9999},viewer)
        for (const root of ['/api/config/tax','/api/config/salary/formula']) {
            const query = root.endsWith('/tax') ? '&type=999' : ''
            const body = await (await request.get(`${root}/fetch?pageSize=500${query}`,{headers:viewer})).json()
            expect(body.code).toBe(0)
            expect(body.data.records.length).toBeGreaterThan(0)
            await denied(request,'put',`${root}/update`,body.data.records[0],viewer)
        }
        expect(fixture('snapshot',bookA)).toEqual(before)
    })
    test('抵扣表真实 Excel 导入仅清理 A，只读用户导入不改变数据', async ({request}) => {
        const upload = {excelFile:{name:'empty-deductions.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',buffer:Buffer.from(fixture('empty-tax-import',bookA).file,'base64')}}
        const before = fixture('snapshot',bookA)
        const rejected = await (await request.post('/api/employee/taxdeduction/import',{headers:viewer,multipart:upload})).json()
        expect(rejected.code).toBe(500014)
        expect(fixture('snapshot',bookA)).toEqual(before)
        const imported = await (await request.post('/api/employee/taxdeduction/import',{headers,multipart:upload})).json()
        expect(imported.code,imported.message).toBe(0)
        const rows = fixture('snapshot',bookA).employee_tax_deduction
        expect(rows.find((row:any) => row.id === own.taxDeduction)?.deleted).toBe('y')
    })
    test('撤销授权后原会话不能继续读取账套或刷新令牌', async ({request}) => {
        const revoked = await (await request.delete(`/api/book/members/revoke?bookId=${bookA}&userId=${viewerId}`,{headers})).json()
        expect(revoked.code,revoked.message).toBe(0)
        try {
            await denied(request,'get',`/api/journal/account/get/${own.account}`,undefined,viewer)
            await denied(request,'get',`/api/users/switchBook/${bookA}`,undefined,viewer)
            const refreshed = await request.post(`/api/auth/token/refresh?refresh_token=${encodeURIComponent(viewerToken)}`,{headers:viewer})
            expect(refreshed.status()).toBe(401)
        } finally {
            expect((await (await request.post('/api/book/members/grant',{headers,data:{bookId:bookA,userId:viewerId,roleId:'ROLE_VIEWER'}})).json()).code).toBe(0)
        }
    })
})
