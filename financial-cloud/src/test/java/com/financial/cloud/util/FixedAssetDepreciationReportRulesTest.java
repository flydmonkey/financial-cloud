package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedAssetDepreciationReportRulesTest {

    @Test
    void openingAccum_sumsDeprBeforeStartPlusCardOpening() {
        Map<String, BigDecimal> byPeriod = Map.of(
                "2026-01", bd("100"),
                "2026-02", bd("100"),
                "2026-03", bd("100")
        );
        assertEquals(0, bd("250").compareTo(
                FixedAssetDepreciationReportRules.openingAccum(bd("50"), byPeriod, "2026-03")));
    }

    @Test
    void periodDepr_sumsInclusiveRange() {
        Map<String, BigDecimal> byPeriod = Map.of(
                "2026-01", bd("10"),
                "2026-02", bd("20"),
                "2026-03", bd("30")
        );
        assertEquals(0, bd("50").compareTo(
                FixedAssetDepreciationReportRules.periodDepr(byPeriod, "2026-02", "2026-03")));
    }

    @Test
    void yearDepr_fromJanToEndOfSameYear() {
        Map<String, BigDecimal> byPeriod = Map.of(
                "2025-12", bd("99"),
                "2026-01", bd("10"),
                "2026-02", bd("20"),
                "2026-03", bd("30")
        );
        assertEquals(0, bd("60").compareTo(
                FixedAssetDepreciationReportRules.yearDepr(byPeriod, "2026-03")));
    }

    @Test
    void endingNetValue() {
        assertEquals(0, bd("8500").compareTo(
                FixedAssetDepreciationReportRules.endingNetValue(bd("10000"), bd("1000"), bd("500"))));
    }

    @Test
    void periodLabel_chineseMonth() {
        assertEquals("2026年08月折旧", FixedAssetDepreciationReportRules.periodDeprColumnLabel("2026-08"));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
