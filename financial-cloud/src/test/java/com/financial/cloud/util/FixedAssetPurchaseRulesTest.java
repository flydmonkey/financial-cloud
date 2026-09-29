package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FixedAssetPurchaseRulesTest {

    @Test
    void creditAmount_addsTax() {
        assertEquals(new BigDecimal("11300.00"),
                FixedAssetPurchaseRules.creditAmount(new BigDecimal("10000"), new BigDecimal("1300")));
    }

    @Test
    void shouldCreateVoucher_falseWhenZero() {
        assertFalse(FixedAssetPurchaseRules.shouldCreateVoucher(BigDecimal.ZERO, null));
        assertTrue(FixedAssetPurchaseRules.shouldCreateVoucher(new BigDecimal("1"), BigDecimal.ZERO));
    }

    @Test
    void skipVoucherReason_whenZeroCredit() {
        assertEquals("原值与税额合计为0，未生成购入凭证",
                FixedAssetPurchaseRules.skipVoucherReason(BigDecimal.ZERO, null));
        assertNull(FixedAssetPurchaseRules.skipVoucherReason(new BigDecimal("1"), BigDecimal.ZERO));
    }
}
