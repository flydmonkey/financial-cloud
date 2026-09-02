package com.financial.cloud.util;

import com.financial.cloud.domain.voucher.Voucher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VoucherUtilsTest {

    @Test
    void createWord_usesHeadDashNum() {
        assertEquals("记-9", VoucherUtils.createWord("记", 2026, 8, 9));
        assertEquals("记-9", VoucherUtils.createWord("记", 9));
        assertEquals("收-100", VoucherUtils.createWord("收", 100));
    }

    @Test
    void displayWord_prefersHeadAndNum() {
        Voucher voucher = Voucher.builder()
                .wordHead("记")
                .wordNum(9)
                .word("记202608第0009号")
                .build();
        assertEquals("记-9", VoucherUtils.displayWord(voucher));
        assertEquals("记-9", VoucherUtils.displayWord("记", 9, "记202608第0009号"));
        assertEquals("记202608第0009号", VoucherUtils.displayWord(null, 9, "记202608第0009号"));
        assertNull(VoucherUtils.displayWord(null));
    }
}
