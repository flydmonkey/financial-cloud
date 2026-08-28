package com.financial.cloud.util;

import com.financial.cloud.dto.statement.StatementExpenseDetailItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementExpenseDetailRulesTest {
    @Test
    void yearLabel_sameYear() {
        assertEquals("2023年合计",
            StatementExpenseDetailRules.yearLabel(List.of("2023-01", "2023-12")));
    }

    @Test
    void yearLabel_crossYear() {
        assertEquals("区间合计",
            StatementExpenseDetailRules.yearLabel(List.of("2023-12", "2024-01")));
    }

    @Test
    void rollup_sumsChildren() {
        var child = item("5601.01", "房租", Map.of("2023-01", bd("100"), "2023-02", bd("50")));
        var parent = item("5601", "销售费用", new HashMap<>());
        parent.setChildren(List.of(child));
        StatementExpenseDetailRules.rollup(parent, List.of("2023-01", "2023-02"));
        assertEquals(0, bd("100").compareTo(parent.getAmounts().get("2023-01")));
        assertEquals(0, bd("50").compareTo(parent.getAmounts().get("2023-02")));
        assertEquals(0, bd("150").compareTo(parent.getYearTotal()));
    }

    @Test
    void computeTotals_sumsRootRows() {
        var a = item("5601", "销售", Map.of("2023-01", bd("10")));
        a.setYearTotal(bd("10"));
        var b = item("5602", "管理", Map.of("2023-01", bd("20")));
        b.setYearTotal(bd("20"));
        var totals = StatementExpenseDetailRules.computeTotals(List.of(a, b), List.of("2023-01"));
        assertEquals(0, bd("30").compareTo(totals.get("2023-01")));
        assertEquals(0, bd("30").compareTo(totals.get("yearTotal")));
    }

    @Test
    void filterZeroLeaves_removesZeroOnlyLeaves() {
        var zero = item("5601.01", "零", Map.of("2023-01", BigDecimal.ZERO));
        zero.setYearTotal(BigDecimal.ZERO);
        var nonzero = item("5601.02", "有", Map.of("2023-01", bd("1")));
        nonzero.setYearTotal(bd("1"));
        var parent = item("5601", "销", new HashMap<>());
        parent.setChildren(new ArrayList<>(List.of(zero, nonzero)));
        StatementExpenseDetailRules.rollup(parent, List.of("2023-01"));
        StatementExpenseDetailRules.filterZeroLeaves(List.of(parent), List.of("2023-01"));
        assertEquals(1, parent.getChildren().size());
        assertEquals("5601.02", parent.getChildren().get(0).getSubjectCode());
    }

    @Test
    void filterZeroLeaves_keepsLeafWithOffsettingPeriodAmounts() {
        var offsetting = item("5601.01", "跨期抵销",
                Map.of("2023-01", bd("100"), "2023-02", bd("-100")));
        var parent = item("5601", "销", new HashMap<>());
        parent.setChildren(new ArrayList<>(List.of(offsetting)));
        StatementExpenseDetailRules.rollup(parent, List.of("2023-01", "2023-02"));

        StatementExpenseDetailRules.filterZeroLeaves(
                List.of(parent), List.of("2023-01", "2023-02"));

        assertEquals(1, parent.getChildren().size());
        assertEquals("5601.01", parent.getChildren().get(0).getSubjectCode());
    }

    @Test
    void reconcile_emitsWarningWhenDiffExceedsTolerance() {
        var detail = Map.of("5601", Map.of("2023-01", bd("100")));
        var income = Map.of("5601", Map.of("2023-01", bd("100.02")));
        var warnings = StatementExpenseDetailRules.reconcile(
            detail, income, new BigDecimal("0.01"));
        assertEquals(1, warnings.size());
        assertEquals("5601", warnings.get(0).getSubjectCode());
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
    private static StatementExpenseDetailItem item(String code, String name, Map<String, BigDecimal> amounts) {
        return StatementExpenseDetailItem.builder()
            .subjectCode(code).subjectName(name)
            .amounts(new HashMap<>(amounts))
            .children(new ArrayList<>())
            .build();
    }
}
