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
}
