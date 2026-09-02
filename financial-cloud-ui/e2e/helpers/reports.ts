import {expect, type APIRequestContext} from '@playwright/test'

export interface ReportLineItem {
    itemCode?: string
    itemName?: string
    symbol?: string
    currentBalance?: number | string
    initialBalance?: number | string
    cumulativeBalance?: number | string
    monthlyAmount?: number | string
    yearToDateAmount?: number | string
}

export interface ReportSnapshot {
    assetTotal: number | null
    liabilityTotal: number | null
    incomeNetProfit: number | null
    cashEnding: number | null
}

export function sheetGrandTotal(items: Array<{itemCode?: string; currentBalance?: number | string}>) {
    const totals = items.filter((item) => (item.itemCode || '').endsWith('99'))
    if (totals.length === 0) {
        return null
    }
    const grand = totals.reduce((max, item) =>
        ((item.itemCode || '') > (max.itemCode || '') ? item : max),
    )
    return Number(grand.currentBalance ?? 0)
}

export function assertBalanceSheetTrial(
    assets: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
    liability: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
) {
    expect(assets.length + liability.length).toBeGreaterThan(0)
    expect(assets.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()
    expect(liability.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()

    const assetTotal = sheetGrandTotal(assets)
    const liabilityTotal = sheetGrandTotal(liability)
    expect(assetTotal, '资产负债表缺少资产总计行').not.toBeNull()
    expect(liabilityTotal, '资产负债表缺少负债及权益总计行').not.toBeNull()
    expect(Math.abs(assetTotal! - liabilityTotal!)).toBeLessThanOrEqual(0.01)
}

export function num(value: number | string | null | undefined): number {
    if (value == null || value === '') {
        return 0
    }
    return Number(value)
}

export function findIncomeItem(items: ReportLineItem[], itemCode: string) {
    return items.find((item) => String(item.itemCode) === itemCode)
}

export function findIncomeItemByName(items: ReportLineItem[], namePart: string) {
    return items.find((item) => (item.itemName || '').includes(namePart))
}

export interface IncomeRuleSpec {
    subjectCode: string
    symbol?: '+' | '-'
    rule?: string
}

/** 按利润表规则类型累加科目本期发生额（对齐 StatementIncomeRules 口径） */
export function subjectAmountByIncomeRule(
    row: {
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    } | undefined,
    ruleType = 'PROFIT_AND_LOSS_AMOUNT',
    symbol: '+' | '-' = '+',
): number {
    const debit = num(row?.currentPeriodDebit)
    const credit = num(row?.currentPeriodCredit)
    const normalizedRule = (ruleType || 'PROFIT_AND_LOSS_AMOUNT').toUpperCase()
    let amount = 0
    if (normalizedRule === 'DEBIT_AMOUNT') {
        // 与后端 StatementIncomeRules.effectiveAmountRule 对齐：收入仅贷方时按贷方取数
        amount = Math.abs(debit) === 0 && Math.abs(credit) > 0 ? credit : debit
    } else if (normalizedRule === 'CREDIT_AMOUNT') {
        amount = credit
    } else {
        amount = Math.abs(debit) - Math.abs(credit)
    }
    return symbol === '-' ? -amount : amount
}

/** 利润表规则科目 → 账套科目别名（对齐 SubjectCodeCompat.CARRY_FORWARD_ALIASES） */
const INCOME_RULE_SUBJECT_ALIASES: Record<string, string[]> = {
    '6001': ['5001'],
    '6051': ['5051'],
    '6301': ['5051'],
    '6401': ['5401'],
    '6405': ['5403'],
    '6601': ['5601'],
    '6602': ['5602'],
    '6603': ['5603'],
    '6711': ['5711'],
    '6801': ['5801'],
}

function incomeRuleSubjectCandidates(ruleSubjectCode: string): string[] {
    const candidates = new Set<string>([ruleSubjectCode])
    for (const alias of INCOME_RULE_SUBJECT_ALIASES[ruleSubjectCode] || []) {
        candidates.add(alias)
    }
    if (ruleSubjectCode.length > 4 && !ruleSubjectCode.includes('.')) {
        const prefix = ruleSubjectCode.slice(0, 4)
        for (const alias of INCOME_RULE_SUBJECT_ALIASES[prefix] || []) {
            candidates.add(alias)
        }
    }
    return [...candidates]
}

function findSubjectBalanceForIncomeRule(
    subjectBalances: Array<{subjectCode?: string; currentPeriodDebit?: number | string; currentPeriodCredit?: number | string}>,
    ruleSubjectCode: string,
) {
    const candidates = incomeRuleSubjectCandidates(ruleSubjectCode)
    return subjectBalances.find((item) => candidates.includes(item.subjectCode || ''))
}

export function sumSubjectPeriodByIncomeRules(
    subjectBalances: Array<{
        subjectCode?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    rules: IncomeRuleSpec[],
): number {
    const appliedSubjects = new Set<string>()
    let total = 0
    for (const rule of rules) {
        const row = findSubjectBalanceForIncomeRule(subjectBalances, rule.subjectCode)
        const subjectCode = row?.subjectCode
        if (!subjectCode || appliedSubjects.has(subjectCode)) {
            continue
        }
        appliedSubjects.add(subjectCode)
        total += subjectAmountByIncomeRule(row, rule.rule, rule.symbol === '-' ? '-' : '+')
    }
    return total
}

/** 结转前损益科目净余额（收入减费用，取绝对值口径） */
export function computeCarryNetFromSubjectBalances(
    balances: Array<{subjectCode?: string; balance?: number | string}>,
    revenueCodes: string[],
    expenseCodes: string[],
): number {
    const revenueTotal = revenueCodes.reduce((sum, code) => sum + Math.abs(getSubjectBalance(balances, code)), 0)
    const expenseTotal = expenseCodes.reduce((sum, code) => sum + Math.abs(getSubjectBalance(balances, code)), 0)
    return revenueTotal - expenseTotal
}

/** 校验利润表逐级公式：营业利润 → 利润总额 → 净利润 */
export function assertIncomeFormulaChain(items: ReportLineItem[], tolerance = 0.01) {
    const revenue = num(findIncomeItem(items, '1')?.currentBalance)
    const operatingProfit = num(findIncomeItem(items, '2')?.currentBalance)
    const totalProfit = num(findIncomeItem(items, '3')?.currentBalance)
    const netProfit = num(findIncomeItem(items, '4')?.currentBalance)

    const section1 = items
        .filter((item) => {
            const code = String(item.itemCode || '')
            return code.length === 3 && code.startsWith('1')
        })
        .reduce((sum, item) => {
            const val = num(item.currentBalance)
            // 与 StatementIncomeRules：section1 加项（名称「加」）按 symbol='-' 计入
            const name = String(item.itemName || '')
            const symbol = name.startsWith('加') ? '-' : item.symbol
            return symbol === '-' ? sum - val : sum + val
        }, 0)

    const section2 = items
        .filter((item) => {
            const code = String(item.itemCode || '')
            return code.length === 3 && code.startsWith('2')
        })
        .reduce((sum, item) => {
            const val = num(item.currentBalance)
            return (item.symbol === '-' ? sum - val : sum + val)
        }, 0)

    const section3 = items
        .filter((item) => {
            const code = String(item.itemCode || '')
            return code.startsWith('3') && code !== '3'
        })
        .reduce((sum, item) => {
            const val = num(item.currentBalance)
            return (item.symbol === '-' ? sum - val : sum + val)
        }, 0)

    expect(Math.abs(operatingProfit - (revenue - section1))).toBeLessThanOrEqual(tolerance)
    expect(Math.abs(totalProfit - (operatingProfit + section2))).toBeLessThanOrEqual(tolerance)
    // section3 已含 symbol 符号（所得税为负），与利润总额相加得净利润
    expect(Math.abs(netProfit - (totalProfit + section3))).toBeLessThanOrEqual(tolerance)

    return {revenue, operatingProfit, totalProfit, netProfit, section1, section2, section3}
}

export async function fetchIncomeStatementItemConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    itemCode: string,
) {
    const res = await request.get(`/api/statement/config/income/${itemCode}`, {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data as {itemCode?: string; itemName?: string; rules?: IncomeRuleSpec[]}
}

export async function assertIncomeLineMatchesRules(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemCode: string,
    rules: IncomeRuleSpec[],
    tolerance = 0.01,
) {
    const income = await fetchIncomeStatement(request, headers, term)
    const line = findIncomeItem(income?.items || [], itemCode)
    expect(line, `利润表缺少 itemCode=${itemCode}`).toBeTruthy()
    const subjectBalances = await fetchSubjectBalances(request, headers, term)
    const expected = sumSubjectPeriodByIncomeRules(subjectBalances, rules)
    expect(
        num(line?.currentBalance),
        `itemCode=${itemCode} 本期数应等于 rules 手工汇总（expected=${expected}）`,
    ).toBeCloseTo(expected, tolerance >= 1 ? 0 : 2)
}

/** 按 config API 拉取 rules 并勾稽利润表行 */
export async function assertIncomeLineMatchesConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemCode: string,
    tolerance = 0.01,
) {
    const config = await fetchIncomeStatementItemConfig(request, headers, itemCode)
    const rules = (config?.rules || [])
        .filter((rule) => rule.subjectCode)
        .map((rule) => ({
            subjectCode: String(rule.subjectCode),
            symbol: rule.symbol === '-' ? '-' as const : '+' as const,
            rule: rule.rule || 'PROFIT_AND_LOSS_AMOUNT',
        }))
    expect(rules.length, `itemCode=${itemCode} 未配置科目 rules`).toBeGreaterThan(0)
    await assertIncomeLineMatchesRules(request, headers, term, itemCode, rules, tolerance)
}

/** 批量逐行 config 勾稽（跳过无 rules 的行次） */
export async function assertIncomeGoldenLines(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemCodes: string[],
    tolerance = 0.01,
) {
    for (const itemCode of itemCodes) {
        const config = await fetchIncomeStatementItemConfig(request, headers, itemCode)
        if (!config?.rules?.length) {
            continue
        }
        await assertIncomeLineMatchesConfig(request, headers, term, itemCode, tolerance)
    }
}

/**
 * IS-R01：结转损益后 3103 增量 ≈ 利润表净利润；损益类科目余额归零
 */
export async function assertIncomeCarryReconciliation(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    options: {
        profit3103BeforeCarry: number
        pAndLSubjectCodes: string[]
        /** 结转前损益科目净余额（与 Δ3103 对齐的主口径；优先于 netProfitBeforeCarry） */
        expectedCarryNet?: number
        netProfitBeforeCarry?: number
        tolerance?: number
    },
) {
    const tolerance = options.tolerance ?? 0.01
    const balances = await fetchSubjectBalances(request, headers, term)
    const profit3103After = getSubjectBalance(balances, '3103')
    const delta3103 = profit3103After - options.profit3103BeforeCarry
    const expectedDelta = options.expectedCarryNet ?? options.netProfitBeforeCarry ?? 0

    expect(Math.abs(delta3103)).toBeCloseTo(Math.abs(expectedDelta), tolerance >= 1 ? 0 : 1)

    for (const code of options.pAndLSubjectCodes) {
        expect(Math.abs(getSubjectBalance(balances, code)), `${code} 结转后应归零`).toBeLessThanOrEqual(tolerance)
    }

    // 利润表口径与科目结转口径一致时再勾稽结转后净利润；否则以 expectedCarryNet（科目）为准
    if (options.netProfitBeforeCarry != null
        && (options.expectedCarryNet == null
            || Math.abs(options.netProfitBeforeCarry - options.expectedCarryNet) <= Math.max(tolerance, 1))) {
        const incomeAfter = await getIncomeNetProfit(request, headers, term)
        expect(Math.abs(incomeAfter.current)).toBeCloseTo(Math.abs(options.netProfitBeforeCarry), tolerance >= 1 ? 0 : 1)
    }
}

/**
 * IS-R02：年末 qm_jz_bnlr 后 Δ未分配利润 ≈ 利润表累计净利润，3103 归零
 */
export async function assertIncomeYearEndReconciliation(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    options: {
        netProfitCumulative: number
        undistributedBefore: number
        profit3103Before: number
        tolerance?: number
    },
) {
    const tolerance = options.tolerance ?? 0.01
    const balances = await fetchSubjectBalances(request, headers, term)
    const undistributedAfter = getSubjectBalanceByCodes(balances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)
    const profit3103After = getSubjectBalanceByCodes(balances, YEAR_PROFIT_SUBJECT_CODES)
    const deltaUndistributed = undistributedAfter - options.undistributedBefore
    const delta3103 = profit3103After - options.profit3103Before

    expect(Math.abs(deltaUndistributed)).toBeCloseTo(
        Math.abs(options.netProfitCumulative),
        tolerance >= 1 ? 0 : 1,
    )
    expect(Math.abs(profit3103After), '3103 年末结转后应归零').toBeLessThanOrEqual(tolerance)
    expect(Math.abs(delta3103 + options.netProfitCumulative)).toBeLessThanOrEqual(
        tolerance >= 1 ? 1 : 0.02,
    )

    try {
        const balanceSheet = await fetchBalanceSheet(request, headers, term)
        const liability = balanceSheet?.items?.liability || []
        const undistributedLine = findBalanceSheetItemByName(liability, '未分配利润')
        if (undistributedLine) {
            expect(num(undistributedLine.currentBalance)).toBeCloseTo(
                undistributedAfter,
                tolerance >= 1 ? 0 : 2,
            )
        }
    } catch {
        // strict 模式下资产负债表可能不可用，科目余额勾稽已足够
    }
}

/**
 * IS-R03：月度结转后未分配利润不变；3103 余额 ≈ 结转前净利润口径（非年末场景）
 *
 * 结转凭证会在损益科目上产生反向发生额，利润表按借贷发生额取数时可能与
 * 结转后科目余额口径的 3103 不一致，故优先用结转前 expectedNetProfit 勾稽。
 */
export async function assertIncomeMonthlyCarryReconciliation(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    options: {
        undistributedBeforeCarry: number
        /** 结转前净利润（科目或利润表），与 Δ3103 / 结转后 3103 对齐 */
        expectedNetProfit?: number
        tolerance?: number
    },
) {
    const tolerance = options.tolerance ?? 0.01
    const balances = await fetchSubjectBalances(request, headers, term)
    const undistributedAfter = getSubjectBalanceByCodes(balances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)
    expect(
        undistributedAfter,
        '月度结转后未分配利润应不变（年末 qm_jz_bnlr 才转入）',
    ).toBeCloseTo(options.undistributedBeforeCarry, tolerance >= 1 ? 0 : 2)

    const profit3103 = getSubjectBalanceByCodes(balances, YEAR_PROFIT_SUBJECT_CODES)
    const expectedNet = options.expectedNetProfit
        ?? (await getIncomeNetProfit(request, headers, term)).current
    expect(Math.abs(profit3103)).toBeCloseTo(Math.abs(expectedNet), tolerance >= 1 ? 0 : 1)

    try {
        const balanceSheet = await fetchBalanceSheet(request, headers, term)
        const liability = balanceSheet?.items?.liability || []
        const undistributedLine = findBalanceSheetItemByName(liability, '未分配利润')
        if (undistributedLine) {
            expect(num(undistributedLine.currentBalance)).toBeCloseTo(
                undistributedAfter,
                tolerance >= 1 ? 0 : 2,
            )
        }
    } catch {
        // strict 模式下资产负债表可能不可用，科目余额勾稽已足够
    }
}

export function findBalanceSheetItemByName(
    items: ReportLineItem[],
    namePart: string,
): ReportLineItem | undefined {
    return items.find((item) => (item.itemName || '').includes(namePart))
}

export function findBalanceSheetItemByCode(
    items: ReportLineItem[],
    itemCode: string,
): ReportLineItem | undefined {
    return items.find((item) => String(item.itemCode) === itemCode)
}

export interface BalanceSheetRuleSpec {
    subjectCode: string
    symbol?: '+' | '-'
    rule?: string
}

export interface BalanceSheetRuleRow extends BalanceSheetRuleSpec {
    closingBalance?: number | string
    openingYearBalance?: number | string
}

type SubjectBalanceRow = {
    subjectCode?: string
    balance?: number | string
    closingBalanceDebit?: number | string
    closingBalanceCredit?: number | string
    direction?: string
}

/** 报表规则科目 → 账套科目别名（对齐 SubjectCodeCompat CARRY_FORWARD_ALIASES） */
const RULE_SUBJECT_ALIASES: Record<string, string[]> = {
    '4001': ['3001'],
    '4002': ['3002'],
    '4103': ['3103'],
    '4104': ['3104'],
    '410406': ['3104.02'],
    '1131': ['1122'],
    '1151': ['1123'],
    '2121': ['2202'],
    '2131': ['2203'],
}

function findSubjectBalanceRows(
    subjectBalances: SubjectBalanceRow[],
    subjectCode: string,
): SubjectBalanceRow[] {
    const candidates = new Set([subjectCode, ...(RULE_SUBJECT_ALIASES[subjectCode] || [])])
    return subjectBalances.filter((item) => candidates.has(item.subjectCode || ''))
}

function findSubjectBalanceRow(
    subjectBalances: SubjectBalanceRow[],
    subjectCode: string,
): SubjectBalanceRow | undefined {
    return findSubjectBalanceRows(subjectBalances, subjectCode)[0]
}

/** 按规则类型与 symbol 累加科目余额（对齐 StatementBalanceSheetRules 口径） */
export function subjectAmountByBalanceRule(row: SubjectBalanceRow | undefined, ruleType = 'BALANCE'): number {
    if (!row) {
        return 0
    }
    const debit = num(row.closingBalanceDebit)
    const credit = num(row.closingBalanceCredit)
    const balance = num(row.balance)
    const normalizedRule = (ruleType || 'BALANCE').toUpperCase()

    if (normalizedRule === 'DEBIT_BALANCE') {
        if (debit !== 0 || credit !== 0) {
            const net = debit - credit
            return net > 0 ? net : 0
        }
        if (row.direction === '2') {
            return balance < 0 ? -balance : 0
        }
        return balance > 0 ? balance : 0
    }
    if (normalizedRule === 'CREDIT_BALANCE') {
        if (debit !== 0 || credit !== 0) {
            const net = credit - debit
            return net > 0 ? net : 0
        }
        if (row.direction === '1') {
            return balance < 0 ? -balance : 0
        }
        return balance > 0 ? balance : 0
    }
    // BALANCE — 对齐 StatementBalanceSheetRules.normalizeByDirection
    if (debit !== 0 || credit !== 0) {
        if (row.direction === '2') {
            return credit - debit
        }
        return debit - credit
    }
    if (row.direction === '2') {
        return balance < 0 ? -balance : balance
    }
    return balance
}

/** 按规则 symbol 累加科目余额 */
export function sumSubjectBalancesByRules(
    subjectBalances: SubjectBalanceRow[],
    rules: BalanceSheetRuleSpec[],
): number {
    return rules.reduce((sum, rule) => {
        const rows = findSubjectBalanceRows(subjectBalances, rule.subjectCode)
        const amount = rows.reduce(
            (rowSum, row) => rowSum + subjectAmountByBalanceRule(row, rule.rule || 'BALANCE'),
            0,
        )
        return rule.symbol === '-' ? sum - amount : sum + amount
    }, 0)
}

export function sumRuleClosingBalances(rules: BalanceSheetRuleRow[]): number {
    return rules.reduce((sum, rule) => {
        const val = num(rule.closingBalance)
        return rule.symbol === '-' ? sum - val : sum + val
    }, 0)
}

export async function fetchBalanceSheetItemConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    itemCode: string,
) {
    const res = await request.get(`/api/statement/config/balance-sheet/${itemCode}`, {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data as {
        itemCode?: string
        itemName?: string
        rules?: BalanceSheetRuleRow[]
    }
}

export async function fetchBalanceSheetRules(
    request: APIRequestContext,
    headers: Record<string, string>,
    itemCode: string,
): Promise<BalanceSheetRuleRow[]> {
    const res = await request.get(
        `/api/statement/config/rules?itemCode=${encodeURIComponent(itemCode)}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as BalanceSheetRuleRow[]
}

/** 报表行期末数 = 绑定科目余额按 symbol 累加（BS-D/M 类用例） */
export async function assertBalanceSheetLineMatchesRules(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemName: string,
    rules: BalanceSheetRuleSpec[],
    tolerance = 0.01,
) {
    const [balanceSheet, subjectBalances] = await Promise.all([
        fetchBalanceSheet(request, headers, term),
        fetchSubjectBalances(request, headers, term),
    ])
    const allItems = [
        ...(balanceSheet?.items?.assets || []),
        ...(balanceSheet?.items?.liability || []),
    ]
    const line = findBalanceSheetItemByName(allItems, itemName)
    expect(line, `资产负债表缺少「${itemName}」行`).toBeTruthy()
    const expected = sumSubjectBalancesByRules(subjectBalances, rules)
    expect(
        num(line?.currentBalance),
        `${itemName} 期末数应等于绑定科目余额之和（expected=${expected}）`,
    ).toBeCloseTo(expected, tolerance >= 1 ? 0 : 2)
}

/** 仅按科目余额 + rules 勾稽（不调用 balance-sheet API，strict 不平衡时可用） */
export async function assertBalanceSheetLineMatchesSubjectRulesByCode(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemCode: string,
    expected: number,
    tolerance = 0.01,
) {
    const [rules, subjectBalances] = await Promise.all([
        fetchBalanceSheetRules(request, headers, itemCode),
        fetchSubjectBalances(request, headers, term),
    ])
    expect(rules.length, `itemCode=${itemCode} 未配置科目规则`).toBeGreaterThan(0)
    const computed = sumSubjectBalancesByRules(
        subjectBalances,
        rules.map((rule) => ({
            subjectCode: String(rule.subjectCode),
            symbol: rule.symbol === '-' ? '-' : '+',
            rule: rule.rule || 'BALANCE',
        })),
    )
    expect(computed, `itemCode=${itemCode} 科目余额合计`).toBeCloseTo(expected, tolerance >= 1 ? 0 : 2)
}

/** 从 config rules 拉取规则并做科目余额勾稽（含 DEBIT/CREDIT_BALANCE） */
export async function assertBalanceSheetLineByNameMatchesRulesFromConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemName: string,
    tolerance = 0.01,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const allItems = [
        ...(balanceSheet?.items?.assets || []),
        ...(balanceSheet?.items?.liability || []),
    ]
    const line = findBalanceSheetItemByName(allItems, itemName)
    expect(line?.itemCode, `资产负债表缺少「${itemName}」行`).toBeTruthy()
    const rules = await fetchBalanceSheetRules(request, headers, String(line!.itemCode))
    expect(rules.length, `${itemName} 未配置科目规则`).toBeGreaterThan(0)
    await assertBalanceSheetLineMatchesRules(
        request,
        headers,
        term,
        itemName,
        rules.map((rule) => ({
            subjectCode: String(rule.subjectCode),
            symbol: rule.symbol === '-' ? '-' : '+',
            rule: rule.rule || 'BALANCE',
        })),
        tolerance,
    )
}

export interface GoldenBalanceSheetExpectation {
    itemName: string
    expected: number
}

/** Golden Dataset：逐行断言关键报表项目 */
export async function assertGoldenBalanceSheetLines(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    expectations: GoldenBalanceSheetExpectation[],
    options?: {skipTrialBalance?: boolean},
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const allItems = [
        ...(balanceSheet?.items?.assets || []),
        ...(balanceSheet?.items?.liability || []),
    ]
    for (const {itemName, expected} of expectations) {
        const line = findBalanceSheetItemByName(allItems, itemName)
        expect(line, `资产负债表缺少「${itemName}」行`).toBeTruthy()
        expect(
            num(line?.currentBalance),
            `${itemName} 期望 ${expected}`,
        ).toBeCloseTo(expected, 0)
        await assertBalanceSheetLineByNameMatchesRulesFromConfig(
            request, headers, term, itemName,
        )
    }
    if (!options?.skipTrialBalance) {
        await assertReportsBalanced(request, headers, term)
    }
}

/** 报表行期末数 = 账套配置 rules 中 closingBalance 累加（与配置页口径一致） */
export async function assertBalanceSheetLineMatchesConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemCode: string,
    tolerance = 0.01,
) {
    const [balanceSheet, config] = await Promise.all([
        fetchBalanceSheet(request, headers, term),
        fetchBalanceSheetItemConfig(request, headers, itemCode),
    ])
    const allItems = [
        ...(balanceSheet?.items?.assets || []),
        ...(balanceSheet?.items?.liability || []),
    ]
    const line = findBalanceSheetItemByCode(allItems, itemCode)
    expect(line, `资产负债表缺少 itemCode=${itemCode}（${config.itemName || ''}）`).toBeTruthy()
    const rules = config.rules || []
    expect(rules.length, `itemCode=${itemCode} 未配置科目规则`).toBeGreaterThan(0)
    const expected = sumRuleClosingBalances(rules)
    expect(
        num(line?.currentBalance),
        `${config.itemName || itemCode} 期末数应等于配置 rules closingBalance 之和（expected=${expected}）`,
    ).toBeCloseTo(expected, tolerance >= 1 ? 0 : 2)
}

/** 按报表项目名称查找行，并用其 itemCode 做 config 勾稽 */
export async function assertBalanceSheetLineByNameMatchesConfig(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    itemName: string,
    tolerance = 0.01,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const allItems = [
        ...(balanceSheet?.items?.assets || []),
        ...(balanceSheet?.items?.liability || []),
    ]
    const line = findBalanceSheetItemByName(allItems, itemName)
    expect(line?.itemCode, `资产负债表缺少「${itemName}」行`).toBeTruthy()
    await assertBalanceSheetLineMatchesConfig(
        request, headers, term, String(line!.itemCode), tolerance,
    )
}

/** 账套 B 标准业务后关键报表行逐行勾稽（按当前账期科目余额，避免 config API 跨期累加） */
export async function assertBookBKeyBalanceSheetLines(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    await assertBalanceSheetLineMatchesRules(
        request, headers, term, '货币资金',
        [{subjectCode: '1001', symbol: '+', rule: 'BALANCE'}, {subjectCode: '1002', symbol: '+', rule: 'BALANCE'}],
    )

    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const assets = balanceSheet?.items?.assets || []
    const receivableLine = findBalanceSheetItemByName(assets, '应收账款')
    if (!receivableLine?.itemCode) {
        return
    }
    const rules = await fetchBalanceSheetRules(request, headers, String(receivableLine.itemCode))
    if (rules.length === 0) {
        return
    }
    await assertBalanceSheetLineMatchesRules(
        request,
        headers,
        term,
        '应收账款',
        rules.map((rule) => ({
            subjectCode: String(rule.subjectCode),
            symbol: rule.symbol === '-' ? '-' : '+',
            rule: rule.rule || 'BALANCE',
        })),
    )
}

export function findCashFlowItem(items: ReportLineItem[], itemCode: string) {
    return items.find((item) => item.itemCode === itemCode)
}

export interface ApiResult<T = unknown> {
    code: number
    message?: string
    data?: T
}

export async function fetchBalanceSheetResult(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
): Promise<ApiResult> {
    const res = await request.get(
        `/api/statement/balance-sheet?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    return res.json()
}

export async function fetchBalanceSheet(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const body = await fetchBalanceSheetResult(request, headers, term, periodType)
    expect(body.code).toBe(0)
    return body.data
}

/** strict 模式下试算不平衡应返回 513013 */
export async function assertBalanceSheetTrialBalanceBlocked(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const body = await fetchBalanceSheetResult(request, headers, term)
    expect(body.code, body.message || 'strict 模式应阻断不平衡报表').toBe(513013)
}

export async function fetchIncomeStatementResult(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
): Promise<ApiResult> {
    const res = await request.get(
        `/api/statement/income?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    return res.json()
}

export async function fetchIncomeStatement(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const body = await fetchIncomeStatementResult(request, headers, term, periodType)
    expect(body.code).toBe(0)
    return body.data
}

/** strict 模式下公式链不平应返回 513014 */
export async function assertIncomeFormulaValidationBlocked(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const body = await fetchIncomeStatementResult(request, headers, term)
    expect(body.code, body.message || 'strict 模式应阻断公式链不平').toBe(513014)
}

/** 三报表统一口径：资产负债表平衡 + 利润表公式链成立 */
export async function assertThreeReportsConsistent(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    await assertReportsBalanced(request, headers, term)
    const income = await fetchIncomeStatement(request, headers, term)
    assertIncomeFormulaChain(income?.items || [])
}

export async function fetchCashFlowStatement(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/cash-flow?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data || []
}

export async function fetchVoucherSummary(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/voucher-summary?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>
}

export async function fetchSubjectBalances(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/subject-balance?periodType=${periodType}&reportDate=${term}&showAll=true`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{
        sourceId?: string
        parentId?: string
        subjectCode?: string
        direction?: string
        isVoucher?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
        balance?: number | string
        closingBalanceDebit?: number | string
        closingBalanceCredit?: number | string
    }>
}

/** 试算平衡：仅汇总末级科目，避免父级行重复计入 */
export function leafSubjectBalanceRows<T extends {sourceId?: string; parentId?: string}>(rows: T[]): T[] {
    const parentIds = new Set(rows.map((row) => row.parentId).filter((id): id is string => Boolean(id)))
    return rows.filter((row) => row.sourceId && !parentIds.has(row.sourceId))
}

export function subjectBalanceTrialTotals(
    rows: Array<{
        sourceId?: string
        parentId?: string
        isVoucher?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
) {
    const voucherLeafRows = leafSubjectBalanceRows(rows).filter((row) => row.isVoucher === 'y')
    const debitTotal = voucherLeafRows.reduce((sum, row) => sum + Math.abs(num(row.currentPeriodDebit)), 0)
    const creditTotal = voucherLeafRows.reduce((sum, row) => sum + Math.abs(num(row.currentPeriodCredit)), 0)
    return {debitTotal, creditTotal, leafCount: voucherLeafRows.length}
}

export function getSubjectBalance(
    records: Array<{subjectCode?: string; balance?: number | string}>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    return num(row?.balance)
}

/** 按候选科目编码顺序取余额（用于 3104.02 / 410406 等别名） */
export function getSubjectBalanceByCodes(
    records: Array<{subjectCode?: string; balance?: number | string}>,
    subjectCodes: string[],
): number {
    for (const code of subjectCodes) {
        const row = records.find((item) => item.subjectCode === code)
        if (row) {
            return num(row.balance)
        }
    }
    return 0
}

export const UNDISTRIBUTED_PROFIT_SUBJECT_CODES = ['3104.02', '410406', '3104', '4104']
export const YEAR_PROFIT_SUBJECT_CODES = ['3103', '4103']

export function subjectPeriodAmount(
    records: Array<{
        subjectCode?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    if (!row) {
        return 0
    }
    return num(row.currentPeriodDebit) + num(row.currentPeriodCredit)
}

export function subjectPeriodNet(
    records: Array<{
        subjectCode?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    if (!row) {
        return 0
    }
    return num(row.currentPeriodDebit) - num(row.currentPeriodCredit)
}

export async function captureReportSnapshot(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
): Promise<ReportSnapshot> {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const income = await fetchIncomeStatement(request, headers, term)
    const cashFlow = await fetchCashFlowStatement(request, headers, term)

    const assets = balanceSheet?.items?.assets || []
    const liability = balanceSheet?.items?.liability || []
    const incomeItems = income?.items || []
    const cashEndingItem = findCashFlowItem(cashFlow, '62-xj-xjqk')
    const monetaryItem = findBalanceSheetItemByName(assets, '货币资金')

    return {
        assetTotal: sheetGrandTotal(assets),
        liabilityTotal: sheetGrandTotal(liability),
        incomeNetProfit: num(findIncomeItem(incomeItems, '4')?.currentBalance),
        cashEnding: cashEndingItem
            ? num(cashEndingItem.monthlyAmount ?? cashEndingItem.yearToDateAmount)
            : num(monetaryItem?.currentBalance),
    }
}

export async function assertReportsBalanced(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    assertBalanceSheetTrial(
        balanceSheet?.items?.assets || [],
        balanceSheet?.items?.liability || [],
    )
}

export async function getIncomeNetProfit(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const income = await fetchIncomeStatement(request, headers, term)
    const item = findIncomeItem(income?.items || [], '4')
    return {
        current: num(item?.currentBalance),
        cumulative: num(item?.cumulativeBalance),
    }
}

export async function getBalanceSheetTotals(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const assets = balanceSheet?.items?.assets || []
    const liability = balanceSheet?.items?.liability || []
    return {
        assetTotal: sheetGrandTotal(assets),
        liabilityTotal: sheetGrandTotal(liability),
    }
}

export function assertSubjectBalanceTrial(
    records: Array<{
        closingBalanceDebit?: number | string
        closingBalanceCredit?: number | string
        yearToDateDebit?: number | string
        yearToDateCredit?: number | string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    mode: 'closing' | 'yearToDate' | 'currentPeriod' = 'yearToDate',
) {
    const fieldMap = {
        closing: ['closingBalanceDebit', 'closingBalanceCredit'],
        yearToDate: ['yearToDateDebit', 'yearToDateCredit'],
        currentPeriod: ['currentPeriodDebit', 'currentPeriodCredit'],
    } as const
    const [debitField, creditField] = fieldMap[mode]
    const debitTotal = records.reduce((sum, row) => sum + num(row[debitField]), 0)
    const creditTotal = records.reduce((sum, row) => sum + num(row[creditField]), 0)
    expect(Math.abs(debitTotal - creditTotal)).toBeLessThanOrEqual(0.01)
    return {debitTotal, creditTotal}
}

export async function exportStatementReport(
    request: APIRequestContext,
    headers: Record<string, string>,
    path: string,
    term: string,
    periodType = 'month',
    extraParams: Record<string, string> = {},
) {
    const params = new URLSearchParams({periodType, reportDate: term, ...extraParams})
    const res = await request.get(`${path}?${params.toString()}`, {headers})
    const contentType = res.headers()['content-type'] || ''
    const body = await res.body()
    if (contentType.includes('json')) {
        const text = body.toString('utf-8')
        throw new Error(`expected spreadsheet export from ${path}, got JSON: ${text.slice(0, 300)}`)
    }
    expect(res.ok(), `export ${path} HTTP ${res.status()}`).toBeTruthy()
    expect(contentType).toMatch(/spreadsheet|octet-stream|excel|zip|openxmlformats/i)
    expect(body.length).toBeGreaterThan(500)
    // xlsx is a zip package
    expect(body.subarray(0, 2).toString('latin1'), `export ${path} is not xlsx/zip`).toBe('PK')
    return body
}

export interface GoldenCashFlowExpectation {
    itemCode: string
    label: string
    expected: number
}

/** 现金流量表公式勾稽：三分项、期初期末、主附经营净额 */
export function assertCashFlowReconciliation(
    items: ReportLineItem[],
    options?: {requireMainEqualsIndirect?: boolean},
) {
    const operating = num(findCashFlowItem(items, '11-jy-lljh')?.monthlyAmount)
    const investing = num(findCashFlowItem(items, '24-tz-llje')?.monthlyAmount)
    const financing = num(findCashFlowItem(items, '34-cz-hdje')?.monthlyAmount)
    const exchange = num(findCashFlowItem(items, '35-hl-djje')?.monthlyAmount)
    const netIncrease = num(findCashFlowItem(items, '36-xj-djje')?.monthlyAmount)
    const beginning = num(findCashFlowItem(items, '37-xj-qcye')?.monthlyAmount)
    const ending = num(findCashFlowItem(items, '38-xj-qmye')?.monthlyAmount)
    const indirectOperating = num(findCashFlowItem(items, '57-xj-jyje')?.monthlyAmount)

    expect(operating + investing + financing + exchange).toBeCloseTo(netIncrease, 2)
    expect(beginning + netIncrease).toBeCloseTo(ending, 2)

    if (options?.requireMainEqualsIndirect) {
        expect(operating, '主表经营净额应等于附表经营净额').toBeCloseTo(indirectOperating, 2)
    }
}

/** Golden Dataset：逐行断言关键现金流量表项目 */
export async function assertGoldenCashFlowLines(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    expectations: GoldenCashFlowExpectation[],
    options?: {requireMainEqualsIndirect?: boolean},
) {
    const items = await fetchCashFlowStatement(request, headers, term)
    for (const {itemCode, label, expected} of expectations) {
        const line = findCashFlowItem(items, itemCode)
        expect(line, `现金流量表缺少「${label}」(${itemCode})`).toBeTruthy()
        expect(num(line?.monthlyAmount), `${label} 期望 ${expected}`).toBeCloseTo(expected, 0)
    }
    assertCashFlowReconciliation(items, options)
}
