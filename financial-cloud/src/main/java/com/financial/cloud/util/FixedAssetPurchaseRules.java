package com.financial.cloud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 固定资产购入凭证金额。
 */
public final class FixedAssetPurchaseRules {

    private FixedAssetPurchaseRules() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 贷方金额 = 原值 + 税额 */
    public static BigDecimal creditAmount(BigDecimal originalValue, BigDecimal taxAmount) {
        return nz(originalValue).add(nz(taxAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean shouldCreateVoucher(BigDecimal originalValue, BigDecimal taxAmount) {
        return creditAmount(originalValue, taxAmount).compareTo(BigDecimal.ZERO) > 0;
    }
}
