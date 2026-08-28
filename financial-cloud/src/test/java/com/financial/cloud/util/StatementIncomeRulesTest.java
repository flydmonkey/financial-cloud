package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementIncomeRulesTest {

    @Test
    void debitAmountRule_returnsDebitOnly() {
        assertEquals(0, bd("10000").compareTo(
                StatementIncomeRules.normalizePeriodAmount(bd("10000"), bd("0"), StatementIncomeRules.DEBIT_AMOUNT)));
    }

    @Test
    void creditAmountRule_returnsCreditOnly() {
        assertEquals(0, bd("80000").compareTo(
                StatementIncomeRules.normalizePeriodAmount(bd("0"), bd("80000"), StatementIncomeRules.CREDIT_AMOUNT)));
    }

    @Test
    void profitAndLossAmountRule_usesAbsNet() {
        assertEquals(0, bd("3000").compareTo(
                StatementIncomeRules.normalizePeriodAmount(bd("5000"), bd("2000"), StatementIncomeRules.PROFIT_AND_LOSS_AMOUNT)));
        assertEquals(0, bd("3000").compareTo(
                StatementIncomeRules.normalizePeriodAmount(bd("-5000"), bd("-2000"), StatementIncomeRules.PROFIT_AND_LOSS_AMOUNT)));
    }

    @Test
    void applyRuleContribution_minusSymbolNegatesAmount() {
        assertEquals(0, bd("-10000").compareTo(
                StatementIncomeRules.applyRuleContribution(
                        bd("10000"), bd("0"), StatementIncomeRules.DEBIT_AMOUNT, StatementSymbolEnum.MINUS.getValue())));
    }

    @Test
    void calculateDerivedLines_bookBScenario() {
        List<StatementIncomeItem> items = new ArrayList<>();
        items.add(line("1", "+", bd("80000"), bd("80000")));
        items.add(line("101", "+", bd("0"), bd("0")));
        items.add(line("105", "+", bd("10000"), bd("10000")));
        items.add(line("2", "+", BigDecimal.ZERO, BigDecimal.ZERO));
        items.add(line("3", "+", BigDecimal.ZERO, BigDecimal.ZERO));
        items.add(line("301", "+", bd("0"), bd("0")));
        items.add(line("4", "+", BigDecimal.ZERO, BigDecimal.ZERO));

        StatementIncomeRules.calculateDerivedLines(items);

        assertEquals(0, bd("70000").compareTo(find(items, "2").getCurrentBalance()));
        assertEquals(0, bd("70000").compareTo(find(items, "3").getCurrentBalance()));
        assertEquals(0, bd("70000").compareTo(find(items, "4").getCurrentBalance()));
        assertEquals(0, bd("70000").compareTo(find(items, "2").getCumulativeBalance()));

        StatementIncomeRules.FormulaChainDiff diff = StatementIncomeRules.computeFormulaChainDiff(items);
        assertEquals(true, diff.withinTolerance());
    }

    @Test
    void computeFormulaChainDiff_detectsCorruptedNetProfit() {
        List<StatementIncomeItem> items = new ArrayList<>();
        items.add(line("1", "+", bd("80000"), bd("80000")));
        items.add(line("101", "+", bd("0"), bd("0")));
        items.add(line("105", "+", bd("10000"), bd("10000")));
        items.add(line("2", "+", bd("70000"), bd("70000")));
        items.add(line("3", "+", bd("70000"), bd("70000")));
        items.add(line("4", "+", bd("60000"), bd("60000")));

        StatementIncomeRules.FormulaChainDiff diff = StatementIncomeRules.computeFormulaChainDiff(items);
        assertEquals(false, diff.withinTolerance());
        assertEquals(0, bd("10000").compareTo(diff.maxAbsDiff()));
    }

    @Test
    void calculateDerivedLines_withNonOperatingAndTax() {
        List<StatementIncomeItem> items = new ArrayList<>();
        items.add(line("1", "+", bd("100000"), bd("100000")));
        items.add(line("101", "+", bd("60000"), bd("60000")));
        items.add(line("105", "+", bd("10000"), bd("10000")));
        items.add(line("103", "+", bd("5000"), bd("5000")));
        items.add(line("2", "+", BigDecimal.ZERO, BigDecimal.ZERO));
        items.add(line("201", "+", bd("1000"), bd("1000")));
        items.add(line("202", "-", bd("500"), bd("500")));
        items.add(line("3", "+", BigDecimal.ZERO, BigDecimal.ZERO));
        items.add(line("301", "+", bd("6500"), bd("6500")));
        items.add(line("4", "+", BigDecimal.ZERO, BigDecimal.ZERO));

        StatementIncomeRules.calculateDerivedLines(items);

        // 营业利润 = 100000 - 60000 - 10000 - 5000 = 25000
        assertEquals(0, bd("25000").compareTo(find(items, "2").getCurrentBalance()));
        // 利润总额 = 25000 + 1000 - 500 = 25500
        assertEquals(0, bd("25500").compareTo(find(items, "3").getCurrentBalance()));
        // 净利润 = 25500 - 6500 = 19000
        assertEquals(0, bd("19000").compareTo(find(items, "4").getCurrentBalance()));
    }

    @Test
    void normalizeIncomeRuleType_revenueDebitBecomesCredit() {
        assertEquals(StatementIncomeRules.CREDIT_AMOUNT,
                StatementIncomeRules.normalizeIncomeRuleType(StatementIncomeRules.DEBIT_AMOUNT, "5001"));
        assertEquals(StatementIncomeRules.CREDIT_AMOUNT,
                StatementIncomeRules.normalizeIncomeRuleType(StatementIncomeRules.DEBIT_AMOUNT, "6001"));
        assertEquals(StatementIncomeRules.DEBIT_AMOUNT,
                StatementIncomeRules.normalizeIncomeRuleType(StatementIncomeRules.DEBIT_AMOUNT, "5602"));
    }

    @Test
    void effectiveAmountRule_creditOnlyPeriodUsesCredit() {
        assertEquals(StatementIncomeRules.CREDIT_AMOUNT,
                StatementIncomeRules.effectiveAmountRule(
                        StatementIncomeRules.DEBIT_AMOUNT, bd("0"), bd("80000")));
        assertEquals(StatementIncomeRules.DEBIT_AMOUNT,
                StatementIncomeRules.effectiveAmountRule(
                        StatementIncomeRules.DEBIT_AMOUNT, bd("10000"), bd("0")));
    }

    private static StatementIncomeItem find(List<StatementIncomeItem> items, String code) {
        return items.stream()
                .filter(item -> code.equals(item.getItemCode()))
                .findFirst()
                .orElseThrow();
    }

    private static StatementIncomeItem line(
            String itemCode, String symbol, BigDecimal current, BigDecimal cumulative) {
        return StatementIncomeItem.builder()
                .itemCode(itemCode)
                .symbol(symbol)
                .currentBalance(current)
                .cumulativeBalance(cumulative)
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
