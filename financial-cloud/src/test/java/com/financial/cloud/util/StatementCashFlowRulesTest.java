package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementCashFlowRulesTest {

    @Test
    void calculateSubtotals_operatingInvestingFinancingEqualsNetIncrease() {
        List<StatementCashFlow> flows = List.of(
                flow("2-jy-sqxj", bd("50000"), bd("50000")),
                flow("6-jy-zfxj", bd("30000"), bd("30000")),
                flow("19-tz-gzxj", bd("20000"), bd("20000")),
                flow("27-cz-qdjk", bd("40000"), bd("40000")),
                flow("30-cz-zfxj", bd("10000"), bd("10000")),
                flow("35-hl-djje", bd("0"), bd("0")));

        Map<String, BigDecimal> results = StatementCashFlowRules.calculateSubtotalsAndNetAmounts(
                flows, true, bd("100000"));

        assertEquals(0, bd("20000").compareTo(results.get("11-jy-lljh")));
        assertEquals(0, bd("-20000").compareTo(results.get("24-tz-llje")));
        assertEquals(0, bd("30000").compareTo(results.get("34-cz-hdje")));
        assertEquals(0, bd("30000").compareTo(results.get("36-xj-djje")));
        assertEquals(0, bd("130000").compareTo(results.get("38-xj-qmye")));
    }

    @Test
    void calculateSubtotals_indirectOtherPlugsToDirectOperatingNet() {
        List<StatementCashFlow> flows = List.of(
                flow("2-jy-sqxj", bd("0"), bd("0")),
                flow("6-jy-zfxj", bd("10000"), bd("10000")),
                flow("41-xj-jlr", bd("-5000"), bd("-5000")),
                flow("43-xj-zczk", bd("5000"), bd("5000")),
                flow("53-xj-chjs", bd("-10000"), bd("-10000")));

        Map<String, BigDecimal> results = StatementCashFlowRules.calculateSubtotalsAndNetAmounts(
                flows, true, bd("100000"));

        assertEquals(0, bd("-10000").compareTo(results.get("11-jy-lljh")));
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get("56-xj-qita")));
        assertEquals(0, bd("-10000").compareTo(results.get("57-xj-jyje")));
        assertTrue(StatementCashFlowRules.isWithinReconciliationTolerance(
                results.get("11-jy-lljh"), results.get("57-xj-jyje")));
    }

    @Test
    void calculateSubtotals_indirectOperatingNet_alwaysMatchesDirectAfterPlug() {
        List<StatementCashFlow> flows = List.of(
                flow("6-jy-zfxj", bd("10000"), bd("10000")),
                flow("41-xj-jlr", bd("-5000"), bd("-5000")),
                flow("43-xj-zczk", bd("5000"), bd("5000")),
                flow("53-xj-chjs", bd("-10000"), bd("-10000")));

        Map<String, BigDecimal> results = StatementCashFlowRules.calculateSubtotalsAndNetAmounts(
                flows, true, bd("100000"));

        assertTrue(StatementCashFlowRules.isWithinReconciliationTolerance(
                results.get("11-jy-lljh"), results.get("57-xj-jyje")));
        assertEquals(0, bd("-10000").compareTo(results.get("57-xj-jyje")));
    }

    @Test
    void strictReconciliation_failsWhenDirectAndIndirectDiffer() {
        assertTrue(StatementCashFlowRules.isWithinReconciliationTolerance(bd("100"), bd("100")));
        assertFalse(StatementCashFlowRules.isWithinReconciliationTolerance(bd("100"), bd("102")));
    }

    private static StatementCashFlow flow(String itemCode, BigDecimal monthly, BigDecimal yearly) {
        StatementCashFlow flow = new StatementCashFlow();
        flow.setItemCode(itemCode);
        flow.setMonthlyAmount(monthly);
        flow.setCurrentAmount(yearly);
        flow.setIsResult(0);
        return flow;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
