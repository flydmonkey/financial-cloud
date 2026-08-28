package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementBalanceSheetRulesTest {

    @Test
    void debitBalanceRule_returnsNetDebitOnly() {
        StatementSubjectBalance creditSideAr = subjectBalance(
                SubjectDirectionEnum.DEBIT.getValue(),
                bd("0"), bd("20000"), bd("-20000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(creditSideAr, StatementBalanceSheetRules.DEBIT_BALANCE)));
    }

    @Test
    void creditBalanceRule_returnsNetCreditOnly() {
        StatementSubjectBalance creditSideAr = subjectBalance(
                SubjectDirectionEnum.DEBIT.getValue(),
                bd("0"), bd("20000"), bd("-20000"));

        assertEquals(0, bd("20000").compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(creditSideAr, StatementBalanceSheetRules.CREDIT_BALANCE)));
    }

    @Test
    void debitBalanceRule_keepsNormalDebitBalance() {
        StatementSubjectBalance normalAr = subjectBalance(
                SubjectDirectionEnum.DEBIT.getValue(),
                bd("30000"), bd("0"), bd("30000"));

        assertEquals(0, bd("30000").compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(normalAr, StatementBalanceSheetRules.DEBIT_BALANCE)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(normalAr, StatementBalanceSheetRules.CREDIT_BALANCE)));
    }

    @Test
    void balanceRule_delegatesToDirectionNormalization() {
        StatementSubjectBalance row = subjectBalance(
                SubjectDirectionEnum.CREDIT.getValue(),
                bd("0"), bd("100000"), bd("100000"));

        assertEquals(0, bd("100000").compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(row, StatementBalanceSheetRules.BALANCE)));
    }

    @Test
    void creditBalanceRule_handlesNegativeDebitClosingField() {
        StatementSubjectBalance creditSideAr = subjectBalance(
                SubjectDirectionEnum.DEBIT.getValue(),
                bd("-15000"), bd("0"), bd("-15000"));

        assertEquals(0, bd("15000").compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(creditSideAr, StatementBalanceSheetRules.CREDIT_BALANCE)));
    }

    @Test
    void balanceRule_creditAccountNetDebit_returnsNegativeNormalized() {
        StatementSubjectBalance row = subjectBalance(
                SubjectDirectionEnum.CREDIT.getValue(),
                bd("5000"), bd("0"), bd("5000"));

        assertEquals(0, bd("-5000").compareTo(
                StatementBalanceSheetRules.normalizeClosingBalance(row, StatementBalanceSheetRules.BALANCE)));
    }

    private static StatementSubjectBalance subjectBalance(
            String direction, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
        return StatementSubjectBalance.builder()
                .direction(direction)
                .closingBalanceDebit(debit)
                .closingBalanceCredit(credit)
                .balance(balance)
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
