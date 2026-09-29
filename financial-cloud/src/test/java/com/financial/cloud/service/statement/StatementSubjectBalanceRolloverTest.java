package com.financial.cloud.service.statement;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementSubjectBalanceRolloverTest {

    private StatementSubjectBalance decemberRow() {
        return StatementSubjectBalance.builder()
                .bookId("b1")
                .yearPeriod("2026-12")
                .subjectCode("1002")
                .openingYearBalanceDebit(new BigDecimal("90000"))   // 年初（2026-01）余额
                .openingYearBalanceCredit(BigDecimal.ZERO)
                .openingBalanceDebit(new BigDecimal("102500"))      // 12 月期初
                .openingBalanceCredit(BigDecimal.ZERO)
                .currentPeriodDebit(new BigDecimal("1500"))
                .currentPeriodCredit(new BigDecimal("500"))
                .yearToDateDebit(new BigDecimal("14000"))
                .yearToDateCredit(new BigDecimal("500"))
                .closingBalanceDebit(new BigDecimal("103500"))      // 2026 年末期末
                .closingBalanceCredit(BigDecimal.ZERO)
                .balance(new BigDecimal("103500"))
                .prevClosingBalanceDebit(BigDecimal.ZERO)
                .prevClosingBalanceCredit(BigDecimal.ZERO)
                .isVoucher("y")
                .build();
    }

    @Test
    void rollover_acrossYearRollsYearOpeningFromPriorYearClosing() {
        StatementSubjectBalance row = decemberRow();
        StatementSubjectBalanceService.rolloverToNextTerm(row, "2027-01");

        assertEquals(new BigDecimal("103500"), row.getOpeningYearBalanceDebit(),
                "跨年后年初余额必须滚存为上年期末");
        assertEquals(new BigDecimal("103500"), row.getOpeningBalanceDebit(), "期初=上月期末");
        assertEquals(BigDecimal.ZERO, row.getYearToDateDebit(), "本年累计清零");
        assertEquals(BigDecimal.ZERO, row.getYearToDateCredit(), "本年累计清零");
        assertEquals(new BigDecimal("103500"), row.getPrevClosingBalanceDebit(),
                "上月期末取自本月期末而非自身旧值");
    }

    @Test
    void rollover_withinYearKeepsYearOpening() {
        StatementSubjectBalance row = decemberRow();
        StatementSubjectBalanceService.rolloverToNextTerm(row, "2026-11");

        assertEquals(new BigDecimal("90000"), row.getOpeningYearBalanceDebit(),
                "年内结账年初余额保持不变");
        assertEquals(new BigDecimal("14000"), row.getYearToDateDebit(), "年内累计不清零");
        assertEquals(new BigDecimal("103500"), row.getPrevClosingBalanceDebit());
        assertEquals("n", row.getIsVoucher());
    }
}
