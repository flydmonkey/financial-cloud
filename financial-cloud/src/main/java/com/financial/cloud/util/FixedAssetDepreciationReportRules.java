package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Map;

/**
 * 固定资产折旧报表金额口径（明细表 / 汇总表共用）。
 */
public final class FixedAssetDepreciationReportRules {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private FixedAssetDepreciationReportRules() {
    }

    /**
     * 期初累计 = 卡片录入期初累计 + 查询起始期之前的全部计提。
     */
    public static BigDecimal openingAccum(BigDecimal cardOpeningAccum,
                                          Map<String, BigDecimal> deprByPeriod,
                                          String startPeriod) {
        BigDecimal sum = nz(cardOpeningAccum);
        if (deprByPeriod == null || StringUtils.isBlank(startPeriod)) {
            return sum.setScale(SCALE, RM);
        }
        for (Map.Entry<String, BigDecimal> e : deprByPeriod.entrySet()) {
            if (FixedAssetDepreciationRules.comparePeriods(e.getKey(), startPeriod) < 0) {
                sum = sum.add(nz(e.getValue()));
            }
        }
        return sum.setScale(SCALE, RM);
    }

    /** 本期折旧 = [start, end] 闭区间计提合计 */
    public static BigDecimal periodDepr(Map<String, BigDecimal> deprByPeriod,
                                        String startPeriod,
                                        String endPeriod) {
        BigDecimal sum = BigDecimal.ZERO;
        if (deprByPeriod == null || StringUtils.isBlank(startPeriod) || StringUtils.isBlank(endPeriod)) {
            return sum.setScale(SCALE, RM);
        }
        for (Map.Entry<String, BigDecimal> e : deprByPeriod.entrySet()) {
            String p = e.getKey();
            if (FixedAssetDepreciationRules.comparePeriods(p, startPeriod) >= 0
                    && FixedAssetDepreciationRules.comparePeriods(p, endPeriod) <= 0) {
                sum = sum.add(nz(e.getValue()));
            }
        }
        return sum.setScale(SCALE, RM);
    }

    /** 本年折旧 = end 所在年 1 月起至 end（含） */
    public static BigDecimal yearDepr(Map<String, BigDecimal> deprByPeriod, String endPeriod) {
        if (StringUtils.isBlank(endPeriod)) {
            return BigDecimal.ZERO.setScale(SCALE, RM);
        }
        YearMonth end = YearMonth.parse(endPeriod);
        String yearStart = end.getYear() + "-01";
        return periodDepr(deprByPeriod, yearStart, endPeriod);
    }

    public static BigDecimal endingAccum(BigDecimal openingAccum, BigDecimal periodDepr) {
        return nz(openingAccum).add(nz(periodDepr)).setScale(SCALE, RM);
    }

    public static BigDecimal endingNetValue(BigDecimal originalValue,
                                            BigDecimal endingAccum,
                                            BigDecimal impairment) {
        return nz(originalValue).subtract(nz(endingAccum)).subtract(nz(impairment)).setScale(SCALE, RM);
    }

    public static String periodDeprColumnLabel(String endPeriod) {
        if (StringUtils.isBlank(endPeriod) || endPeriod.length() < 7) {
            return "本期折旧";
        }
        String y = endPeriod.substring(0, 4);
        String m = endPeriod.substring(5, 7);
        return y + "年" + m + "月折旧";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
