package com.financial.cloud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 固定资产清理凭证金额规则（账面价值、损益方向）。
 */
public final class FixedAssetDisposalRules {

    private FixedAssetDisposalRules() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 账面价值 = 原值 − 累计折旧 − 减值准备（不小于 0） */
    public static BigDecimal bookValue(BigDecimal originalValue, BigDecimal accumDepr, BigDecimal impairment) {
        return nz(originalValue).subtract(nz(accumDepr)).subtract(nz(impairment))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 清理科目结转后余额（借方为正）：账面价值 − 处置收入 + 清理费用。
     * &gt;0 为净损失，&lt;0 为净收益（绝对值）。
     */
    public static BigDecimal clearBalance(BigDecimal bookValue, BigDecimal disposeIncome, BigDecimal disposeExpense) {
        return nz(bookValue).subtract(nz(disposeIncome)).add(nz(disposeExpense))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
