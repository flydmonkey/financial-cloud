package com.financial.cloud.util;

import com.financial.cloud.enums.fixedasset.DepreciationMethod;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FixedAssetDepreciationRulesTest {

    @Test
    void residualValue_scalesHalfUp() {
        assertEquals(0, bd("500.00").compareTo(
                FixedAssetDepreciationRules.residualValue(bd("10000"), bd("5"))));
        assertEquals(0, bd("333.33").compareTo(
                FixedAssetDepreciationRules.residualValue(bd("6666.67"), bd("5"))));
    }

    @Test
    void depreciableBase_subtractsImpairmentAndResidual() {
        assertEquals(0, bd("8500").compareTo(
                FixedAssetDepreciationRules.depreciableBase(bd("10000"), bd("1000"), bd("500"))));
    }

    @Test
    void remainingDepreciable_returnsZeroWhenExhausted() {
        assertEquals(0, bd("0.00").compareTo(
                FixedAssetDepreciationRules.remainingDepreciable(bd("8500"), bd("8500"))));
        assertEquals(0, bd("0.00").compareTo(
                FixedAssetDepreciationRules.remainingDepreciable(bd("8500"), bd("9000"))));
        assertEquals(0, bd("500").compareTo(
                FixedAssetDepreciationRules.remainingDepreciable(bd("8500"), bd("8000"))));
    }

    @Test
    void straightLine_regularMonth() {
        // (10000-500)/60 = 158.33
        BigDecimal residual = FixedAssetDepreciationRules.residualValue(bd("10000"), bd("5"));
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, BigDecimal.ZERO);
        assertEquals(0, bd("158.33").compareTo(
                FixedAssetDepreciationRules.straightLineAmount(bd("10000"), residual, 60, 0, remaining)));
    }

    @Test
    void straightLine_lastPeriodCatchUp() {
        BigDecimal residual = bd("500.00");
        BigDecimal remaining = bd("100.00");
        assertEquals(0, bd("100.00").compareTo(
                FixedAssetDepreciationRules.straightLineAmount(bd("10000"), residual, 60, 59, remaining)));
    }

    @Test
    void straightLine_cappedByRemaining() {
        BigDecimal residual = bd("500.00");
        BigDecimal remaining = bd("50.00");
        assertEquals(0, bd("50.00").compareTo(
                FixedAssetDepreciationRules.straightLineAmount(bd("10000"), residual, 60, 10, remaining)));
    }

    @Test
    void straightLine_zeroWhenNoRemaining() {
        assertEquals(0, bd("0.00").compareTo(
                FixedAssetDepreciationRules.straightLineAmount(bd("10000"), bd("500"), 60, 10, BigDecimal.ZERO)));
    }

    @Test
    void unitsOfProduction_regular() {
        // unit = 9500/10000 = 0.95; amount = 0.95 * 100 = 95.00
        assertEquals(0, bd("95.00").compareTo(
                FixedAssetDepreciationRules.unitsOfProductionAmount(
                        bd("10000"), bd("500"), bd("10000"), bd("100"), bd("9500"))));
    }

    @Test
    void unitsOfProduction_exhaustUsesRemaining() {
        assertEquals(0, bd("80.00").compareTo(
                FixedAssetDepreciationRules.unitsOfProductionAmount(
                        bd("10000"), bd("500"), bd("10000"), bd("100"), bd("80"))));
    }

    @Test
    void shouldAccrue_startNextMonth() {
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-01", "2026-01", null, null, FixedAssetStatus.IN_USE.name()));
        assertTrue(FixedAssetDepreciationRules.shouldAccrue(
                "2026-02", "2026-01", null, null, FixedAssetStatus.IN_USE.name()));
    }

    @Test
    void shouldAccrue_disposedSameMonthStillAccrues() {
        assertTrue(FixedAssetDepreciationRules.shouldAccrue(
                "2026-06", "2026-01", "2026-06", null, FixedAssetStatus.DISPOSED.name()));
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-07", "2026-01", "2026-06", null, FixedAssetStatus.DISPOSED.name()));
    }

    @Test
    void shouldAccrue_disposedWithoutPeriodFalse() {
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-06", "2026-01", null, null, FixedAssetStatus.DISPOSED.name()));
    }

    @Test
    void shouldAccrue_suspendedFromPeriodInclusive() {
        assertTrue(FixedAssetDepreciationRules.shouldAccrue(
                "2026-05", "2026-01", null, "2026-06", FixedAssetStatus.SUSPENDED.name()));
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-06", "2026-01", null, "2026-06", FixedAssetStatus.SUSPENDED.name()));
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-07", "2026-01", null, "2026-06", FixedAssetStatus.SUSPENDED.name()));
    }

    @Test
    void shouldAccrue_suspendedWithoutPeriodFalse() {
        assertFalse(FixedAssetDepreciationRules.shouldAccrue(
                "2026-06", "2026-01", null, null, FixedAssetStatus.SUSPENDED.name()));
    }

    @Test
    void periodHelpers() {
        LocalDate d = LocalDate.of(2026, 3, 15);
        Date date = Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertEquals("2026-03", FixedAssetDepreciationRules.periodOf(date));
        assertEquals("2026-04", FixedAssetDepreciationRules.nextPeriod("2026-03"));
        assertTrue(FixedAssetDepreciationRules.comparePeriods("2026-02", "2026-03") < 0);
        assertEquals(0, FixedAssetDepreciationRules.comparePeriods("2026-03", "2026-03"));
    }

    @Test
    void noneMethod_isNotDepreciable() {
        assertFalse(DepreciationMethod.NONE.isDepreciable());
        assertTrue(DepreciationMethod.STRAIGHT_LINE.isDepreciable());
    }

    @Test
    void acceleratedLife_requiresFullYearsAtLeastTwo() {
        assertTrue(FixedAssetDepreciationRules.isValidAcceleratedLife(24));
        assertTrue(FixedAssetDepreciationRules.isValidAcceleratedLife(60));
        assertFalse(FixedAssetDepreciationRules.isValidAcceleratedLife(12));
        assertFalse(FixedAssetDepreciationRules.isValidAcceleratedLife(30));
        assertFalse(FixedAssetDepreciationRules.isValidAcceleratedLife(null));
    }

    @Test
    void doubleDeclining_firstYearMonthly() {
        // N=5, r=0.4, opening=10000, annual=4000, monthly=333.33
        BigDecimal residual = FixedAssetDepreciationRules.residualValue(bd("10000"), bd("5"));
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, BigDecimal.ZERO);
        assertEquals(0, bd("333.33").compareTo(
                FixedAssetDepreciationRules.doubleDecliningAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 0, BigDecimal.ZERO, remaining)));
    }

    @Test
    void doubleDeclining_secondYearUsesYearOpeningNet() {
        // After year0: accum=4000, year1 opening net=6000, annual=2400, monthly=200.00
        BigDecimal residual = bd("500.00");
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal accum = bd("4000.00");
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, accum);
        assertEquals(0, bd("200.00").compareTo(
                FixedAssetDepreciationRules.doubleDecliningAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 12, accum, remaining)));
    }

    @Test
    void doubleDeclining_lastTwoYearsStraightLine() {
        // After 3 DDB years: accum=7840, remaining=1660, 24 months left → 69.17
        BigDecimal residual = bd("500.00");
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal accum = bd("7840.00");
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, accum);
        assertEquals(0, bd("69.17").compareTo(
                FixedAssetDepreciationRules.doubleDecliningAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 36, accum, remaining)));
    }

    @Test
    void doubleDeclining_lastPeriodCatchUp() {
        BigDecimal residual = bd("500.00");
        BigDecimal remaining = bd("50.00");
        assertEquals(0, bd("50.00").compareTo(
                FixedAssetDepreciationRules.doubleDecliningAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 59, bd("9450.00"), remaining)));
    }

    @Test
    void sumOfYears_firstYearMonthly() {
        // N=5, S=15, base=9500, annual=9500*5/15=3166.67, monthly=263.89
        BigDecimal residual = bd("500.00");
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, BigDecimal.ZERO);
        assertEquals(0, bd("263.89").compareTo(
                FixedAssetDepreciationRules.sumOfYearsAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 0, remaining)));
    }

    @Test
    void sumOfYears_laterYearFactor() {
        // year=1, factor=4, annual=9500*4/15=2533.33, monthly=211.11
        BigDecimal residual = bd("500.00");
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(bd("10000"), BigDecimal.ZERO, residual);
        BigDecimal accum = bd("3166.67");
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, accum);
        assertEquals(0, bd("211.11").compareTo(
                FixedAssetDepreciationRules.sumOfYearsAmount(
                        bd("10000"), BigDecimal.ZERO, residual, 60, 12, remaining)));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
