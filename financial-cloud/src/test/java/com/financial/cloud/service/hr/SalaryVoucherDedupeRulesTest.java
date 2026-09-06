package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryVoucherDedupeRulesTest {

    @Test
    void linkedVoucherWhenEitherIdPresent() {
        assertTrue(SalaryVoucherDedupeRules.hasLinkedVoucher("a", null));
        assertTrue(SalaryVoucherDedupeRules.hasLinkedVoucher(null, "s"));
        assertTrue(SalaryVoucherDedupeRules.hasLinkedVoucher("a", "s"));
        assertFalse(SalaryVoucherDedupeRules.hasLinkedVoucher(null, null));
        assertFalse(SalaryVoucherDedupeRules.hasLinkedVoucher("  ", ""));
    }

    @Test
    void linkedVoucherForTypeUsesAccrualOrPaymentField() {
        assertTrue(SalaryVoucherDedupeRules.hasLinkedVoucherForType(2, "a", null));
        assertFalse(SalaryVoucherDedupeRules.hasLinkedVoucherForType(2, null, "s"));
        assertTrue(SalaryVoucherDedupeRules.hasLinkedVoucherForType(3, null, "s"));
        assertFalse(SalaryVoucherDedupeRules.hasLinkedVoucherForType(3, "a", null));
    }

    @Test
    void pushBlockedWhenAnyLinkedCountPositive() {
        assertTrue(SalaryVoucherDedupeRules.shouldBlockPush(1));
        assertFalse(SalaryVoucherDedupeRules.shouldBlockPush(0));
        assertFalse(SalaryVoucherDedupeRules.shouldBlockPush(-1));
    }

    @Test
    void messages() {
        assertEquals(
                "本月工资明细已生成计提或发放凭证，请先在工资明细中删除对应凭证后再重新推送",
                SalaryVoucherDedupeRules.BLOCK_PUSH_BECAUSE_VOUCHERS);
        assertEquals(
                "该员工本月已生成计提凭证，请勿重复生成",
                SalaryVoucherDedupeRules.generateBlockedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 2));
        assertEquals(
                "该员工本月已生成收票凭证，请勿重复生成",
                SalaryVoucherDedupeRules.generateBlockedMessage(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 2));
        assertEquals(
                "该员工本月已生成发放凭证，请勿重复生成",
                SalaryVoucherDedupeRules.generateBlockedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 3));
    }
}
