package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementCashFlowIndirectRulesTest {

    @Test
    void inventoryIncreaseShowsNegativeDecrease() {
        assertEquals(0, bd("-10000").compareTo(
                StatementCashFlowIndirectRules.inventoryDecrease(bd("50000"), bd("60000"))));
    }

    @Test
    void operatingReceivableDecrease_whenPrepaidUnchanged_isZero() {
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> lines = Map.of(
                StatementCashFlowIndirectRules.BS_PREPAID,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("40000"), bd("40000")),
                StatementCashFlowIndirectRules.BS_ACCOUNTS_RECEIVABLE,
                new StatementCashFlowIndirectRules.ReportLineBalance(BigDecimal.ZERO, BigDecimal.ZERO));
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> prior = lines;

        StatementCashFlowIndirectRules.WorkingCapitalChanges changes =
                StatementCashFlowIndirectRules.computeWorkingCapitalChanges(lines, prior, true);

        assertEquals(0, BigDecimal.ZERO.compareTo(changes.receivableChangePeriod()));
        assertEquals(0, BigDecimal.ZERO.compareTo(changes.payableChangePeriod()));
    }

    @Test
    void goldenDatasetPurchase_updatesInventoryAndKeepsReceivablePayableFlat() {
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> opening = Map.of(
                StatementCashFlowIndirectRules.BS_INVENTORY,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("50000"), bd("60000")),
                StatementCashFlowIndirectRules.BS_PREPAID,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("40000"), bd("40000")),
                StatementCashFlowIndirectRules.BS_ACCOUNTS_RECEIVABLE,
                new StatementCashFlowIndirectRules.ReportLineBalance(BigDecimal.ZERO, BigDecimal.ZERO),
                StatementCashFlowIndirectRules.BS_ADVANCE_RECEIPT,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("20000"), bd("20000")),
                StatementCashFlowIndirectRules.BS_ACCOUNTS_PAYABLE,
                new StatementCashFlowIndirectRules.ReportLineBalance(BigDecimal.ZERO, BigDecimal.ZERO));

        StatementCashFlowIndirectRules.WorkingCapitalChanges changes =
                StatementCashFlowIndirectRules.computeWorkingCapitalChanges(opening, opening, true);

        assertEquals(0, bd("-10000").compareTo(changes.inventoryChangePeriod()));
        assertEquals(0, BigDecimal.ZERO.compareTo(changes.receivableChangePeriod()));
        assertEquals(0, BigDecimal.ZERO.compareTo(changes.payableChangePeriod()));
    }

    @Test
    void sumDepreciationCredit_usesCurrentPeriodCredit() {
        StatementSubjectBalance row = new StatementSubjectBalance();
        row.setSubjectCode("1602");
        row.setCurrentPeriodCredit(bd("5000"));
        row.setYearToDateCredit(bd("5000"));

        assertEquals(0, bd("5000").compareTo(
                StatementCashFlowIndirectRules.sumDepreciationCredit(List.of(row), false)));
    }

    @Test
    void crossMonthOpening_usesPriorPeriodClosingNotYearInitial() {
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> reportLines = Map.of(
                StatementCashFlowIndirectRules.BS_INVENTORY,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("50000"), bd("55000")));
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> priorLines = Map.of(
                StatementCashFlowIndirectRules.BS_INVENTORY,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("40000"), bd("50000")));

        StatementCashFlowIndirectRules.WorkingCapitalChanges changes =
                StatementCashFlowIndirectRules.computeWorkingCapitalChanges(
                        reportLines, priorLines, false);

        // 期初取上月期末 50000，期末 55000 → 53 = -5000
        assertEquals(0, bd("-5000").compareTo(changes.inventoryChangePeriod()));
        // 年累计仍用年初 50000 vs 期末 55000
        assertEquals(0, bd("-5000").compareTo(changes.inventoryChangeYear()));
    }

    @Test
    void crossMonthCashBeginning_usesPriorEndingViaReportService() {
        // 文档/设计层：resolvePeriodOpening 非首月走 prior current
        assertEquals(0, bd("50000").compareTo(
                StatementCashFlowIndirectRules.resolvePeriodOpening(
                        Map.of(StatementCashFlowIndirectRules.BS_INVENTORY,
                                new StatementCashFlowIndirectRules.ReportLineBalance(bd("50000"), bd("60000"))),
                        Map.of(StatementCashFlowIndirectRules.BS_INVENTORY,
                                new StatementCashFlowIndirectRules.ReportLineBalance(bd("40000"), bd("50000"))),
                        StatementCashFlowIndirectRules.BS_INVENTORY,
                        false)));
    }

    @Test
    void financialExpenseAdjustment_passesThroughIncomeStatementAmount() {
        assertEquals(0, bd("1200").compareTo(
                StatementCashFlowIndirectRules.financialExpenseAdjustment(bd("1200"))));
    }

    @Test
    void investmentLossAdjustment_negatesInvestmentIncome() {
        assertEquals(0, bd("-800").compareTo(
                StatementCashFlowIndirectRules.investmentLossAdjustment(bd("800"))));
    }

    @Test
    void exchangeRateEffect_netLossMinusGain() {
        assertEquals(0, bd("300").compareTo(
                StatementCashFlowIndirectRules.exchangeRateEffect(bd("500"), bd("200"))));
    }

    @Test
    void computeSupplementaryAdjustments_derivesImpairmentAmortizationAndDeferredTax() {
        StatementSubjectBalance impairment = balance("1505", bd("0"), bd("200"), bd("0"), bd("200"));
        StatementSubjectBalance amortization = balance("1702", bd("0"), bd("1500"), bd("0"), bd("1500"));
        StatementSubjectBalance deferredExpense = balance("1801", bd("0"), bd("800"), bd("0"), bd("800"));
        StatementSubjectBalance exchangeLoss = balance("5603.02", bd("100"), bd("0"), bd("100"), bd("0"));
        StatementSubjectBalance exchangeGain = balance("5301.05", bd("0"), bd("40"), bd("0"), bd("40"));

        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> lines = Map.of(
                StatementCashFlowIndirectRules.BS_DEFERRED_TAX_ASSET,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("1000"), bd("700")),
                StatementCashFlowIndirectRules.BS_DEFERRED_TAX_LIABILITY,
                new StatementCashFlowIndirectRules.ReportLineBalance(bd("500"), bd("900")));
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> prior = lines;

        StatementCashFlowIndirectRules.SupplementaryAdjustments adj =
                StatementCashFlowIndirectRules.computeSupplementaryAdjustments(
                        List.of(impairment, amortization, deferredExpense, exchangeLoss, exchangeGain),
                        lines,
                        prior,
                        true,
                        bd("600"),
                        bd("600"),
                        bd("250"),
                        bd("250"));

        assertEquals(0, bd("200").compareTo(adj.assetImpairmentPeriod()));
        assertEquals(0, bd("1500").compareTo(adj.amortizationIntangiblePeriod()));
        assertEquals(0, bd("800").compareTo(adj.amortizationDeferredExpensePeriod()));
        assertEquals(0, bd("600").compareTo(adj.financialExpensePeriod()));
        assertEquals(0, bd("-250").compareTo(adj.investmentLossPeriod()));
        assertEquals(0, bd("300").compareTo(adj.deferredTaxAssetDecreasePeriod()));
        assertEquals(0, bd("400").compareTo(adj.deferredTaxLiabilityIncreasePeriod()));
        assertEquals(0, bd("60").compareTo(adj.exchangeRateEffectPeriod()));
    }

    private static StatementSubjectBalance balance(
            String code,
            BigDecimal periodDebit,
            BigDecimal periodCredit,
            BigDecimal yearDebit,
            BigDecimal yearCredit) {
        StatementSubjectBalance row = new StatementSubjectBalance();
        row.setSubjectCode(code);
        row.setCurrentPeriodDebit(periodDebit);
        row.setCurrentPeriodCredit(periodCredit);
        row.setYearToDateDebit(yearDebit);
        row.setYearToDateCredit(yearCredit);
        return row;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
