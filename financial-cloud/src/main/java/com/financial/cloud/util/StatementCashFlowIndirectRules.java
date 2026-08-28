package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 现金流量表附表（间接法）自动调整项：与资产负债表行项同源口径。
 */
public final class StatementCashFlowIndirectRules {

    public static final String BS_INVENTORY = "存货";
    public static final String BS_ACCOUNTS_RECEIVABLE = "应收账款";
    public static final String BS_PREPAID = "预付款项";
    public static final String BS_ACCOUNTS_PAYABLE = "应付账款";
    public static final String BS_ADVANCE_RECEIPT = "预收款项";
    public static final String BS_DEFERRED_TAX_ASSET = "递延所得税资产";
    public static final String BS_DEFERRED_TAX_LIABILITY = "递延所得税负债";

    public static final String ACCUMULATED_DEPRECIATION_CODE = "1602";
    public static final String ACCUMULATED_AMORTIZATION_CODE = "1702";
    public static final String LONG_TERM_DEFERRED_EXPENSE_CODE = "1801";
    public static final String FIXED_ASSET_IMPAIRMENT_CODE = "1505";
    public static final String INTANGIBLE_IMPAIRMENT_CODE = "1805";
    public static final String DEFERRED_TAX_ASSET_CODE = "1811";
    public static final String DEFERRED_TAX_LIABILITY_CODE = "2901";
    public static final String EXCHANGE_LOSS_SUBJECT_CODE = "5603.02";
    public static final String EXCHANGE_GAIN_SUBJECT_CODE = "5301.05";
    public static final String INVESTMENT_INCOME_INCOME_ITEM_CODE = "301";

    public static final List<String> INDIRECT_SUBJECT_ROOTS = List.of(
            ACCUMULATED_DEPRECIATION_CODE,
            ACCUMULATED_AMORTIZATION_CODE,
            LONG_TERM_DEFERRED_EXPENSE_CODE,
            FIXED_ASSET_IMPAIRMENT_CODE,
            INTANGIBLE_IMPAIRMENT_CODE,
            DEFERRED_TAX_ASSET_CODE,
            DEFERRED_TAX_LIABILITY_CODE,
            EXCHANGE_LOSS_SUBJECT_CODE,
            EXCHANGE_GAIN_SUBJECT_CODE);

    private StatementCashFlowIndirectRules() {
    }

    public record ReportLineBalance(BigDecimal initialBalance, BigDecimal currentBalance) {
    }

    public record WorkingCapitalChanges(
            BigDecimal inventoryChangePeriod,
            BigDecimal receivableChangePeriod,
            BigDecimal payableChangePeriod,
            BigDecimal inventoryChangeYear,
            BigDecimal receivableChangeYear,
            BigDecimal payableChangeYear) {
    }

    public record SupplementaryAdjustments(
            BigDecimal assetImpairmentPeriod,
            BigDecimal assetImpairmentYear,
            BigDecimal amortizationIntangiblePeriod,
            BigDecimal amortizationIntangibleYear,
            BigDecimal amortizationDeferredExpensePeriod,
            BigDecimal amortizationDeferredExpenseYear,
            BigDecimal financialExpensePeriod,
            BigDecimal financialExpenseYear,
            BigDecimal investmentLossPeriod,
            BigDecimal investmentLossYear,
            BigDecimal deferredTaxAssetDecreasePeriod,
            BigDecimal deferredTaxAssetDecreaseYear,
            BigDecimal deferredTaxLiabilityIncreasePeriod,
            BigDecimal deferredTaxLiabilityIncreaseYear,
            BigDecimal exchangeRateEffectPeriod,
            BigDecimal exchangeRateEffectYear) {
    }

    /** 存货的减少 = 期初存货 − 期末存货（增加以负号表示）。 */
    public static BigDecimal inventoryDecrease(BigDecimal opening, BigDecimal closing) {
        return defaultZero(opening).subtract(defaultZero(closing));
    }

    /** 经营性应收项目的减少 = 期初 − 期末。 */
    public static BigDecimal operatingReceivableDecrease(BigDecimal opening, BigDecimal closing) {
        return defaultZero(opening).subtract(defaultZero(closing));
    }

    /** 经营性应付项目的增加 = 期末 − 期初。 */
    public static BigDecimal operatingPayableIncrease(BigDecimal opening, BigDecimal closing) {
        return defaultZero(closing).subtract(defaultZero(opening));
    }

    /** 递延所得税资产减少 = 期初 − 期末。 */
    public static BigDecimal deferredTaxAssetDecrease(BigDecimal opening, BigDecimal closing) {
        return inventoryDecrease(opening, closing);
    }

    /** 递延所得税负债增加 = 期末 − 期初。 */
    public static BigDecimal deferredTaxLiabilityIncrease(BigDecimal opening, BigDecimal closing) {
        return operatingPayableIncrease(opening, closing);
    }

    /** 财务费用（加回）= 利润表财务费用行金额。 */
    public static BigDecimal financialExpenseAdjustment(BigDecimal incomeStatementAmount) {
        return defaultZero(incomeStatementAmount);
    }

    /** 投资损失(减：收益) = −投资收益。 */
    public static BigDecimal investmentLossAdjustment(BigDecimal investmentIncome) {
        return defaultZero(investmentIncome).negate();
    }

    /** 汇率变动 = 汇兑损失 − 汇兑收益（净损失为正）。 */
    public static BigDecimal exchangeRateEffect(BigDecimal exchangeLossNet, BigDecimal exchangeGainNet) {
        return defaultZero(exchangeLossNet).subtract(defaultZero(exchangeGainNet));
    }

    public static WorkingCapitalChanges computeWorkingCapitalChanges(
            Map<String, ReportLineBalance> reportLines,
            Map<String, ReportLineBalance> priorPeriodLines,
            boolean firstBookPeriod) {
        BigDecimal inventoryOpenPeriod = resolvePeriodOpening(
                reportLines, priorPeriodLines, BS_INVENTORY, firstBookPeriod);
        BigDecimal inventoryClose = lineCurrent(reportLines, BS_INVENTORY);

        BigDecimal receivableOpenPeriod = resolvePeriodOpeningSum(
                reportLines, priorPeriodLines, firstBookPeriod,
                BS_ACCOUNTS_RECEIVABLE, BS_PREPAID);
        BigDecimal receivableClose = sumLineCurrent(reportLines, BS_ACCOUNTS_RECEIVABLE, BS_PREPAID);

        BigDecimal payableOpenPeriod = resolvePeriodOpeningSum(
                reportLines, priorPeriodLines, firstBookPeriod,
                BS_ACCOUNTS_PAYABLE, BS_ADVANCE_RECEIPT);
        BigDecimal payableClose = sumLineCurrent(reportLines, BS_ACCOUNTS_PAYABLE, BS_ADVANCE_RECEIPT);

        BigDecimal inventoryOpenYear = lineInitial(reportLines, BS_INVENTORY);
        BigDecimal receivableOpenYear = sumLineInitial(reportLines, BS_ACCOUNTS_RECEIVABLE, BS_PREPAID);
        BigDecimal payableOpenYear = sumLineInitial(reportLines, BS_ACCOUNTS_PAYABLE, BS_ADVANCE_RECEIPT);

        return new WorkingCapitalChanges(
                inventoryDecrease(inventoryOpenPeriod, inventoryClose),
                operatingReceivableDecrease(receivableOpenPeriod, receivableClose),
                operatingPayableIncrease(payableOpenPeriod, payableClose),
                inventoryDecrease(inventoryOpenYear, inventoryClose),
                operatingReceivableDecrease(receivableOpenYear, receivableClose),
                operatingPayableIncrease(payableOpenYear, payableClose));
    }

    public static SupplementaryAdjustments computeSupplementaryAdjustments(
            Collection<StatementSubjectBalance> balances,
            Map<String, ReportLineBalance> reportLines,
            Map<String, ReportLineBalance> priorPeriodLines,
            boolean firstBookPeriod,
            BigDecimal financialExpensePeriod,
            BigDecimal financialExpenseYear,
            BigDecimal investmentIncomePeriod,
            BigDecimal investmentIncomeYear) {
        BigDecimal impairmentPeriod = sumSubjectCredits(balances, List.of(
                FIXED_ASSET_IMPAIRMENT_CODE, INTANGIBLE_IMPAIRMENT_CODE), false);
        BigDecimal impairmentYear = sumSubjectCredits(balances, List.of(
                FIXED_ASSET_IMPAIRMENT_CODE, INTANGIBLE_IMPAIRMENT_CODE), true);

        BigDecimal amortizationPeriod = sumSubjectCredits(balances, List.of(ACCUMULATED_AMORTIZATION_CODE), false);
        BigDecimal amortizationYear = sumSubjectCredits(balances, List.of(ACCUMULATED_AMORTIZATION_CODE), true);

        BigDecimal deferredExpensePeriod = sumSubjectCredits(balances,
                List.of(LONG_TERM_DEFERRED_EXPENSE_CODE), false);
        BigDecimal deferredExpenseYear = sumSubjectCredits(balances,
                List.of(LONG_TERM_DEFERRED_EXPENSE_CODE), true);

        BigDecimal dtaOpenPeriod = resolveBalanceOpening(
                reportLines, priorPeriodLines, BS_DEFERRED_TAX_ASSET, DEFERRED_TAX_ASSET_CODE,
                balances, firstBookPeriod);
        BigDecimal dtaClose = resolveBalanceClosing(reportLines, BS_DEFERRED_TAX_ASSET, DEFERRED_TAX_ASSET_CODE, balances);
        BigDecimal dtaOpenYear = lineInitial(reportLines, BS_DEFERRED_TAX_ASSET);
        if (dtaOpenYear.signum() == 0) {
            dtaOpenYear = sumSubjectOpening(balances, DEFERRED_TAX_ASSET_CODE);
        }

        BigDecimal dtlOpenPeriod = resolveBalanceOpening(
                reportLines, priorPeriodLines, BS_DEFERRED_TAX_LIABILITY, DEFERRED_TAX_LIABILITY_CODE,
                balances, firstBookPeriod);
        BigDecimal dtlClose = resolveBalanceClosing(
                reportLines, BS_DEFERRED_TAX_LIABILITY, DEFERRED_TAX_LIABILITY_CODE, balances);
        BigDecimal dtlOpenYear = lineInitial(reportLines, BS_DEFERRED_TAX_LIABILITY);
        if (dtlOpenYear.signum() == 0) {
            dtlOpenYear = sumSubjectOpening(balances, DEFERRED_TAX_LIABILITY_CODE);
        }

        BigDecimal exchangeLossPeriod = sumSubjectNetDebit(balances, List.of(EXCHANGE_LOSS_SUBJECT_CODE), false);
        BigDecimal exchangeLossYear = sumSubjectNetDebit(balances, List.of(EXCHANGE_LOSS_SUBJECT_CODE), true);
        BigDecimal exchangeGainPeriod = sumSubjectNetCredit(balances, List.of(EXCHANGE_GAIN_SUBJECT_CODE), false);
        BigDecimal exchangeGainYear = sumSubjectNetCredit(balances, List.of(EXCHANGE_GAIN_SUBJECT_CODE), true);

        return new SupplementaryAdjustments(
                impairmentPeriod,
                impairmentYear,
                amortizationPeriod,
                amortizationYear,
                deferredExpensePeriod,
                deferredExpenseYear,
                financialExpenseAdjustment(financialExpensePeriod),
                financialExpenseAdjustment(financialExpenseYear),
                investmentLossAdjustment(investmentIncomePeriod),
                investmentLossAdjustment(investmentIncomeYear),
                deferredTaxAssetDecrease(dtaOpenPeriod, dtaClose),
                deferredTaxAssetDecrease(dtaOpenYear, dtaClose),
                deferredTaxLiabilityIncrease(dtlOpenPeriod, dtlClose),
                deferredTaxLiabilityIncrease(dtlOpenYear, dtlClose),
                exchangeRateEffect(exchangeLossPeriod, exchangeGainPeriod),
                exchangeRateEffect(exchangeLossYear, exchangeGainYear));
    }

    /** 固定资产折旧 = 累计折旧科目本期/本年贷方发生额。 */
    public static BigDecimal sumDepreciationCredit(
            Collection<StatementSubjectBalance> balances, boolean yearToDate) {
        return sumSubjectCredits(balances, List.of(ACCUMULATED_DEPRECIATION_CODE), yearToDate);
    }

    static BigDecimal sumSubjectCredits(
            Collection<StatementSubjectBalance> balances, List<String> roots, boolean yearToDate) {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(roots);
        return balances.stream()
                .filter(row -> codes.contains(row.getSubjectCode()))
                .map(row -> yearToDate
                        ? defaultZero(row.getYearToDateCredit())
                        : defaultZero(row.getCurrentPeriodCredit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumSubjectNetDebit(
            Collection<StatementSubjectBalance> balances, List<String> roots, boolean yearToDate) {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(roots);
        return balances.stream()
                .filter(row -> codes.contains(row.getSubjectCode()))
                .map(row -> {
                    BigDecimal debit = yearToDate
                            ? defaultZero(row.getYearToDateDebit())
                            : defaultZero(row.getCurrentPeriodDebit());
                    BigDecimal credit = yearToDate
                            ? defaultZero(row.getYearToDateCredit())
                            : defaultZero(row.getCurrentPeriodCredit());
                    return debit.subtract(credit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumSubjectNetCredit(
            Collection<StatementSubjectBalance> balances, List<String> roots, boolean yearToDate) {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(roots);
        return balances.stream()
                .filter(row -> codes.contains(row.getSubjectCode()))
                .map(row -> {
                    BigDecimal debit = yearToDate
                            ? defaultZero(row.getYearToDateDebit())
                            : defaultZero(row.getCurrentPeriodDebit());
                    BigDecimal credit = yearToDate
                            ? defaultZero(row.getYearToDateCredit())
                            : defaultZero(row.getCurrentPeriodCredit());
                    return credit.subtract(debit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumSubjectOpening(Collection<StatementSubjectBalance> balances, String root) {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(List.of(root));
        return balances.stream()
                .filter(row -> codes.contains(row.getSubjectCode()))
                .map(row -> StatementBalanceSheetRules.normalizeOpeningBalance(
                        row, StatementBalanceSheetRules.BALANCE))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumSubjectClosing(Collection<StatementSubjectBalance> balances, String root) {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(List.of(root));
        return balances.stream()
                .filter(row -> codes.contains(row.getSubjectCode()))
                .map(row -> StatementBalanceSheetRules.normalizeClosingBalance(
                        row, StatementBalanceSheetRules.BALANCE))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal resolveBalanceOpening(
            Map<String, ReportLineBalance> reportLines,
            Map<String, ReportLineBalance> priorPeriodLines,
            String bsLineName,
            String subjectRoot,
            Collection<StatementSubjectBalance> balances,
            boolean firstBookPeriod) {
        BigDecimal fromBs = resolvePeriodOpening(reportLines, priorPeriodLines, bsLineName, firstBookPeriod);
        if (fromBs.signum() != 0 || reportLines.containsKey(bsLineName)) {
            return fromBs;
        }
        return sumSubjectOpening(balances, subjectRoot);
    }

    static BigDecimal resolveBalanceClosing(
            Map<String, ReportLineBalance> reportLines,
            String bsLineName,
            String subjectRoot,
            Collection<StatementSubjectBalance> balances) {
        BigDecimal fromBs = lineCurrent(reportLines, bsLineName);
        if (fromBs.signum() != 0 || reportLines.containsKey(bsLineName)) {
            return fromBs;
        }
        return sumSubjectClosing(balances, subjectRoot);
    }

    static BigDecimal resolvePeriodOpening(
            Map<String, ReportLineBalance> reportLines,
            Map<String, ReportLineBalance> priorPeriodLines,
            String lineName,
            boolean firstBookPeriod) {
        if (firstBookPeriod) {
            return lineInitial(reportLines, lineName);
        }
        return lineCurrent(priorPeriodLines, lineName);
    }

    static BigDecimal resolvePeriodOpeningSum(
            Map<String, ReportLineBalance> reportLines,
            Map<String, ReportLineBalance> priorPeriodLines,
            boolean firstBookPeriod,
            String... lineNames) {
        if (firstBookPeriod) {
            return sumLineInitial(reportLines, lineNames);
        }
        return sumLineCurrent(priorPeriodLines, lineNames);
    }

    static BigDecimal lineInitial(Map<String, ReportLineBalance> lines, String name) {
        ReportLineBalance line = lines.get(name);
        return line != null ? defaultZero(line.initialBalance()) : BigDecimal.ZERO;
    }

    static BigDecimal lineCurrent(Map<String, ReportLineBalance> lines, String name) {
        ReportLineBalance line = lines.get(name);
        return line != null ? defaultZero(line.currentBalance()) : BigDecimal.ZERO;
    }

    static BigDecimal sumLineInitial(Map<String, ReportLineBalance> lines, String... names) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String name : names) {
            sum = sum.add(lineInitial(lines, name));
        }
        return sum;
    }

    static BigDecimal sumLineCurrent(Map<String, ReportLineBalance> lines, String... names) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String name : names) {
            sum = sum.add(lineCurrent(lines, name));
        }
        return sum;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
