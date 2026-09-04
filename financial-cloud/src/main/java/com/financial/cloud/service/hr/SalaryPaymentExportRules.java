package com.financial.cloud.service.hr;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards for bank payment-file export from confirmed salary details.
 */
public final class SalaryPaymentExportRules {

    private SalaryPaymentExportRules() {
    }

    public record PaymentRow(String employeeName, String employeeNumber, String bankName,
                             String bankCardNo, java.math.BigDecimal netPay, String belongDate) {
    }

    public static List<String> missingBankAccountNames(List<PaymentRow> rows) {
        List<String> missing = new ArrayList<>();
        if (rows == null) {
            return missing;
        }
        for (PaymentRow row : rows) {
            if (row.bankCardNo() == null || row.bankCardNo().isBlank()) {
                String name = row.employeeName();
                if (name == null || name.isBlank()) {
                    name = row.employeeNumber() != null ? row.employeeNumber() : "未知员工";
                }
                missing.add(name);
            }
        }
        return missing;
    }

    public static boolean isEmptyMonth(List<PaymentRow> rows) {
        return rows == null || rows.isEmpty();
    }
}
