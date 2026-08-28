package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementGeneralLedgerItem;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementGeneralLedgerRulesTest {

    @Test
    void periodCode_stripsDash() {
        assertEquals("202312", StatementGeneralLedgerRules.periodCode("2023-12"));
    }

    @Test
    void fold_sumsPeriodAcrossMonths_takesFirstOpeningAndLastYtd() {
        StatementSubjectBalance jan = balance("2023-01",
                "100", "0", "50", "0", "50", "0", "150", "0");
        StatementSubjectBalance feb = balance("2023-02",
                "150", "0", "20", "0", "70", "0", "170", "0");

        var folded = StatementGeneralLedgerRules.fold(
                List.of(jan, feb), SubjectDirectionEnum.DEBIT.getValue());

        assertEquals(0, bd("100").compareTo(folded.openingDebit));
        assertEquals(0, bd("70").compareTo(folded.periodDebit));
        assertEquals(0, bd("70").compareTo(folded.ytdDebit));
        assertEquals(0, bd("170").compareTo(folded.closingDebit));
    }

    @Test
    void shouldHide_whenNoActivityAndZeroClosing_defaultFlag() {
        var folded = new StatementGeneralLedgerRules.FoldedBalance();
        folded.direction = SubjectDirectionEnum.DEBIT.getValue();
        assertTrue(StatementGeneralLedgerRules.shouldHideGroup(folded, false, true));
    }

    @Test
    void shouldHide_whenHideZeroBalance_evenWithActivity() {
        var folded = new StatementGeneralLedgerRules.FoldedBalance();
        folded.direction = SubjectDirectionEnum.DEBIT.getValue();
        folded.periodDebit = bd("10");
        folded.periodCredit = bd("10");
        folded.closingDebit = BigDecimal.ZERO;
        folded.closingCredit = BigDecimal.ZERO;
        assertTrue(StatementGeneralLedgerRules.shouldHideGroup(folded, true, false));
        assertFalse(StatementGeneralLedgerRules.shouldHideGroup(folded, false, true));
    }

    @Test
    void expandRows_threeRows_openingDebitCreditNull() {
        var folded = new StatementGeneralLedgerRules.FoldedBalance();
        folded.direction = SubjectDirectionEnum.DEBIT.getValue();
        folded.openingDebit = bd("100");
        folded.periodDebit = bd("50");
        folded.ytdDebit = bd("150");
        folded.closingDebit = bd("150");

        List<StatementGeneralLedgerItem> rows = StatementGeneralLedgerRules.expandRows(
                "1001", "库存现金", "2023-12", folded, false);

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).getRowSpan());
        assertEquals(0, rows.get(1).getRowSpan());
        assertEquals(StatementGeneralLedgerRules.SUMMARY_OPENING, rows.get(0).getSummary());
        assertNull(rows.get(0).getDebit());
        assertNull(rows.get(0).getCredit());
        assertEquals("借", rows.get(0).getDirection());
        assertEquals(0, bd("100").compareTo(rows.get(0).getBalance()));
        assertEquals("202312", rows.get(0).getPeriod());
        assertEquals(StatementGeneralLedgerRules.SUMMARY_PERIOD, rows.get(1).getSummary());
        assertEquals(0, bd("50").compareTo(rows.get(1).getDebit()));
        assertEquals(StatementGeneralLedgerRules.SUMMARY_YTD, rows.get(2).getSummary());
        assertEquals(0, bd("150").compareTo(rows.get(2).getDebit()));
    }

    @Test
    void expandRows_onlyOpening_whenHidePeriodRowsAndNoActivity() {
        var folded = new StatementGeneralLedgerRules.FoldedBalance();
        folded.direction = SubjectDirectionEnum.DEBIT.getValue();
        folded.openingDebit = bd("100");
        folded.closingDebit = bd("100");

        List<StatementGeneralLedgerItem> rows = StatementGeneralLedgerRules.expandRows(
                "1001", "库存现金", "2023-12", folded, true);

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getRowSpan());
        assertEquals(StatementGeneralLedgerRules.SUMMARY_OPENING, rows.get(0).getSummary());
    }

    @Test
    void displayDirection_creditSubjectShows贷() {
        assertEquals("贷", StatementGeneralLedgerRules.displayDirection(
                bd("200"), SubjectDirectionEnum.CREDIT.getValue()));
        assertEquals("借", StatementGeneralLedgerRules.displayDirection(
                bd("-10"), SubjectDirectionEnum.CREDIT.getValue()));
    }

    private static StatementSubjectBalance balance(
            String period,
            String openD, String openC,
            String curD, String curC,
            String ytdD, String ytdC,
            String closeD, String closeC) {
        return StatementSubjectBalance.builder()
                .yearPeriod(period)
                .openingBalanceDebit(bd(openD))
                .openingBalanceCredit(bd(openC))
                .currentPeriodDebit(bd(curD))
                .currentPeriodCredit(bd(curC))
                .yearToDateDebit(bd(ytdD))
                .yearToDateCredit(bd(ytdC))
                .closingBalanceDebit(bd(closeD))
                .closingBalanceCredit(bd(closeC))
                .direction(SubjectDirectionEnum.DEBIT.getValue())
                .build();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
