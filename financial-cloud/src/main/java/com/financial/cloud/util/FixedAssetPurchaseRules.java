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

    /**
     * User-facing reason when no purchase voucher is created. Null when a voucher should be created.
     */
    public static String skipVoucherReason(BigDecimal originalValue, BigDecimal taxAmount) {
        if (shouldCreateVoucher(originalValue, taxAmount)) {
            return null;
        }
        return "原值与税额合计为0，未生成购入凭证";
    }
}
