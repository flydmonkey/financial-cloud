package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedAssetDisposalRulesTest {

    @Test
    void bookValue_subtractsAccumAndImpairment() {
        assertEquals(new BigDecimal("15000.00"),
                FixedAssetDisposalRules.bookValue(bd("20000"), bd("5000"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("14000.00"),
                FixedAssetDisposalRules.bookValue(bd("20000"), bd("5000"), bd("1000")));
    }

    @Test
    void clearBalance_lossWhenIncomeLessThanBook() {
        // NBV 15000, income 10000, expense 500 → loss 5500
        assertEquals(new BigDecimal("5500.00"),
                FixedAssetDisposalRules.clearBalance(bd("15000"), bd("10000"), bd("500")));
    }

    @Test
    void clearBalance_gainWhenIncomeExceedsBook() {
        // NBV 15000, income 20000 → gain -5000
        assertEquals(new BigDecimal("-5000.00"),
                FixedAssetDisposalRules.clearBalance(bd("15000"), bd("20000"), BigDecimal.ZERO));
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
