package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryPaymentExportRulesTest {

    @Test
    void emptyMonthDetected() {
        assertTrue(SalaryPaymentExportRules.isEmptyMonth(List.of()));
        assertTrue(SalaryPaymentExportRules.isEmptyMonth(null));
        assertFalse(SalaryPaymentExportRules.isEmptyMonth(List.of(
                new SalaryPaymentExportRules.PaymentRow("A", "1", "Bank", "6222", BigDecimal.ONE, "2026-08"))));
    }

    @Test
    void missingBankAccountsListed() {
        List<SalaryPaymentExportRules.PaymentRow> rows = List.of(
                new SalaryPaymentExportRules.PaymentRow("有卡", "1", "工行", "6222001", new BigDecimal("100"), "2026-08"),
                new SalaryPaymentExportRules.PaymentRow("无卡", "2", "建行", null, new BigDecimal("200"), "2026-08"),
                new SalaryPaymentExportRules.PaymentRow("空卡", "3", null, "  ", new BigDecimal("50"), "2026-08"));
        List<String> missing = SalaryPaymentExportRules.missingBankAccountNames(rows);
        assertEquals(List.of("无卡", "空卡"), missing);
    }
}
