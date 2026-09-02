package com.financial.cloud.util;

import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;

/**
 * 固定资产折旧计算规则（直线法、工作量法、双倍余额递减、年数总和）。
 */
public final class FixedAssetDepreciationRules {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private FixedAssetDepreciationRules() {
    }

    /** 加速折旧：使用月数 ≥24 且为 12 的整数倍 */
    public static boolean isValidAcceleratedLife(Integer usefulLifeMonths) {
        if (usefulLifeMonths == null || usefulLifeMonths < 24) {
            return false;
        }
        return usefulLifeMonths % 12 == 0;
    }

    /** 预计净残值 = 原值 × 残值率% / 100，两位小数 HALF_UP */
    public static BigDecimal residualValue(BigDecimal originalValue, BigDecimal residualRate) {
        BigDecimal original = nz(originalValue);
        BigDecimal rate = nz(residualRate);
        return original.multiply(rate)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, ROUNDING);
    }

    /** 应提折旧总额 = 原值 − 减值 − 预计净残值 */
    public static BigDecimal depreciableBase(BigDecimal originalValue, BigDecimal impairment, BigDecimal residual) {
        return nz(originalValue).subtract(nz(impairment)).subtract(nz(residual));
    }

    /** 剩余可提 = 应提总额 − 已累计折旧；≤0 则 0 */
    public static BigDecimal remainingDepreciable(BigDecimal depreciableBase, BigDecimal currentAccumDepr) {
        BigDecimal remaining = nz(depreciableBase).subtract(nz(currentAccumDepr));
        return remaining.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING) : remaining;
    }

    /**
     * 直线法本期折旧额。
     * 月折旧 = (原值 − 残值) / 期数；取 min(月折旧, 剩余)；最后一期补差提足。
     */
    public static BigDecimal straightLineAmount(BigDecimal originalValue,
                                               BigDecimal residual,
                                               Integer usefulLifeMonths,
                                               Integer depreciatedPeriods,
                                               BigDecimal remaining) {
        BigDecimal rem = nz(remaining);
        if (rem.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        int months = usefulLifeMonths == null ? 0 : usefulLifeMonths;
        if (months <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        int done = depreciatedPeriods == null ? 0 : depreciatedPeriods;
        if (done + 1 >= months) {
            return rem.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal monthly = nz(originalValue).subtract(nz(residual))
                .divide(BigDecimal.valueOf(months), MONEY_SCALE, ROUNDING);
        return monthly.min(rem).setScale(MONEY_SCALE, ROUNDING);
    }

    /**
     * 工作量法本期折旧额。
     * 单位折旧 = (原值 − 残值) / 预计总工作量；本期 = 单位 × 本期工作量；不超过剩余，触及上限则提足。
     */
    public static BigDecimal unitsOfProductionAmount(BigDecimal originalValue,
                                                    BigDecimal residual,
                                                    BigDecimal expectedTotalWork,
                                                    BigDecimal periodWork,
                                                    BigDecimal remaining) {
        BigDecimal rem = nz(remaining);
        if (rem.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal totalWork = nz(expectedTotalWork);
        if (totalWork.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal work = nz(periodWork);
        if (work.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal unit = nz(originalValue).subtract(nz(residual))
                .divide(totalWork, 10, ROUNDING);
        BigDecimal amount = unit.multiply(work).setScale(MONEY_SCALE, ROUNDING);
        if (amount.compareTo(rem) >= 0) {
            return rem.setScale(MONEY_SCALE, ROUNDING);
        }
        return amount;
    }

    /**
     * 双倍余额递减法本期折旧额（按年、月均摊；最后两年转直线）。
     * currentAccumDepr 保留参数以对齐调用方，金额由模拟年初净值计算。
     */
    public static BigDecimal doubleDecliningAmount(BigDecimal originalValue,
                                                  BigDecimal impairment,
                                                  BigDecimal residual,
                                                  Integer usefulLifeMonths,
                                                  Integer depreciatedPeriods,
                                                  BigDecimal currentAccumDepr,
                                                  BigDecimal remaining) {
        BigDecimal rem = nz(remaining);
        if (rem.compareTo(BigDecimal.ZERO) <= 0 || !isValidAcceleratedLife(usefulLifeMonths)) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        int months = usefulLifeMonths;
        int done = depreciatedPeriods == null ? 0 : Math.max(0, depreciatedPeriods);
        if (done + 1 >= months) {
            return rem.setScale(MONEY_SCALE, ROUNDING);
        }
        int years = months / 12;
        int year = done / 12;
        int monthsLeft = months - done;
        if (year >= years - 2) {
            return rem.divide(BigDecimal.valueOf(monthsLeft), MONEY_SCALE, ROUNDING).min(rem)
                    .setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(years), 10, ROUNDING);
        BigDecimal openingNet = nz(originalValue).subtract(nz(impairment));
        for (int y = 0; y < year; y++) {
            BigDecimal annual = openingNet.multiply(rate).setScale(MONEY_SCALE, ROUNDING);
            openingNet = openingNet.subtract(annual);
        }
        BigDecimal annual = openingNet.multiply(rate).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal monthly = annual.divide(BigDecimal.valueOf(12), MONEY_SCALE, ROUNDING);
        return monthly.min(rem).setScale(MONEY_SCALE, ROUNDING);
    }

    /**
     * 年数总和法本期折旧额（按年、月均摊）。
     */
    public static BigDecimal sumOfYearsAmount(BigDecimal originalValue,
                                             BigDecimal impairment,
                                             BigDecimal residual,
                                             Integer usefulLifeMonths,
                                             Integer depreciatedPeriods,
                                             BigDecimal remaining) {
        BigDecimal rem = nz(remaining);
        if (rem.compareTo(BigDecimal.ZERO) <= 0 || !isValidAcceleratedLife(usefulLifeMonths)) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        int months = usefulLifeMonths;
        int done = depreciatedPeriods == null ? 0 : Math.max(0, depreciatedPeriods);
        if (done + 1 >= months) {
            return rem.setScale(MONEY_SCALE, ROUNDING);
        }
        int years = months / 12;
        int year = done / 12;
        if (year >= years) {
            return rem.setScale(MONEY_SCALE, ROUNDING);
        }
        int sumYears = years * (years + 1) / 2;
        int factor = years - year;
        BigDecimal base = depreciableBase(originalValue, impairment, residual);
        BigDecimal annual = base.multiply(BigDecimal.valueOf(factor))
                .divide(BigDecimal.valueOf(sumYears), MONEY_SCALE, ROUNDING);
        BigDecimal monthly = annual.divide(BigDecimal.valueOf(12), MONEY_SCALE, ROUNDING);
        return monthly.min(rem).setScale(MONEY_SCALE, ROUNDING);
    }

    /**
     * 是否应计提本期折旧。
     * 开始使用当月不提；清理当月仍提、次月停；已清理且无清理期 → 不提；
     * 暂停所属期及之后不提。
     */
    public static boolean shouldAccrue(String yearPeriod,
                                       String startUsePeriod,
                                       String disposedPeriod,
                                       String suspendedPeriod,
                                       String status) {
        if (StringUtils.isBlank(yearPeriod)) {
            return false;
        }
        if (FixedAssetStatus.DISPOSED.name().equalsIgnoreCase(status) && StringUtils.isBlank(disposedPeriod)) {
            return false;
        }
        if (StringUtils.isNotBlank(disposedPeriod) && comparePeriods(yearPeriod, disposedPeriod) > 0) {
            return false;
        }
        if (StringUtils.isNotBlank(suspendedPeriod) && comparePeriods(yearPeriod, suspendedPeriod) >= 0) {
            return false;
        }
        if (FixedAssetStatus.SUSPENDED.name().equalsIgnoreCase(status) && StringUtils.isBlank(suspendedPeriod)) {
            return false;
        }
        if (StringUtils.isNotBlank(startUsePeriod) && comparePeriods(yearPeriod, startUsePeriod) <= 0) {
            return false;
        }
        return true;
    }

    /** @deprecated 使用带 suspendedPeriod 的重载 */
    @Deprecated
    public static boolean shouldAccrue(String yearPeriod,
                                       String startUsePeriod,
                                       String disposedPeriod,
                                       String status) {
        return shouldAccrue(yearPeriod, startUsePeriod, disposedPeriod, null, status);
    }

    /** 日期所属会计期 yyyy-MM */
    public static String periodOf(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return YearMonth.from(localDate).toString();
    }

    public static String periodOf(LocalDate date) {
        if (date == null) {
            return null;
        }
        return YearMonth.from(date).toString();
    }

    /** 下一会计期 */
    public static String nextPeriod(String period) {
        if (StringUtils.isBlank(period)) {
            return period;
        }
        return YearMonth.parse(period).plusMonths(1).toString();
    }

    /** 比较两个 yyyy-MM；a&lt;b 负，相等 0，a&gt;b 正 */
    public static int comparePeriods(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return YearMonth.parse(a).compareTo(YearMonth.parse(b));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
