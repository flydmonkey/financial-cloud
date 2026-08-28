package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementGeneralLedgerItem;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对照「总账计算规则」文档的公式级断言（不依赖 DB）。
 */
class StatementGeneralLedgerRulesCalcRulesTest {

    /** 规则四：资产类 期末 = 期初 + 本期借 − 本期贷 */
    @Test
    void balanceFormula_assetDebitSubject() {
        var folded = foldOneMonth(
                SubjectDirectionEnum.DEBIT.getValue(),
                "1000", "0",   // opening
                "300", "50",   // period
                "1250", "0");  // closing stored
        BigDecimal expected = bd("1000").add(bd("300")).subtract(bd("50"));
        assertEquals(0, expected.compareTo(
                StatementGeneralLedgerRules.signedClosingFromOpeningAndPeriod(folded)));
        assertEquals(0, expected.compareTo(
                StatementGeneralLedgerRules.signedBalance(
                        folded.closingDebit, folded.closingCredit, folded.direction)));
    }

    /** 规则四：负债/权益类 期末 = 期初 + 本期贷 − 本期借 */
    @Test
    void balanceFormula_liabilityCreditSubject() {
        var folded = foldOneMonth(
                SubjectDirectionEnum.CREDIT.getValue(),
                "0", "2000",
                "100", "400",
                "0", "2300");
        BigDecimal expected = bd("2000").add(bd("400")).subtract(bd("100"));
        assertEquals(0, expected.compareTo(
                StatementGeneralLedgerRules.signedClosingFromOpeningAndPeriod(folded)));
        assertEquals(0, expected.compareTo(
                StatementGeneralLedgerRules.signedBalance(
                        folded.closingDebit, folded.closingCredit, folded.direction)));
    }

    /** 规则三：本期发生额 = 各月本期之和；本年累计取末日 YTD */
    @Test
    void periodAndYtd_occurrenceRules() {
        StatementSubjectBalance jan = StatementSubjectBalance.builder()
                .yearPeriod("2026-01")
                .openingBalanceDebit(bd("100"))
                .openingBalanceCredit(BigDecimal.ZERO)
                .currentPeriodDebit(bd("10"))
                .currentPeriodCredit(bd("2"))
                .yearToDateDebit(bd("10"))
                .yearToDateCredit(bd("2"))
                .closingBalanceDebit(bd("108"))
                .closingBalanceCredit(BigDecimal.ZERO)
                .build();
        StatementSubjectBalance feb = StatementSubjectBalance.builder()
                .yearPeriod("2026-02")
                .openingBalanceDebit(bd("108"))
                .openingBalanceCredit(BigDecimal.ZERO)
                .currentPeriodDebit(bd("20"))
                .currentPeriodCredit(bd("5"))
                .yearToDateDebit(bd("30"))
                .yearToDateCredit(bd("7"))
                .closingBalanceDebit(bd("123"))
                .closingBalanceCredit(BigDecimal.ZERO)
                .build();

        var folded = StatementGeneralLedgerRules.fold(
                List.of(jan, feb), SubjectDirectionEnum.DEBIT.getValue());

        assertEquals(0, bd("100").compareTo(folded.openingDebit));
        assertEquals(0, bd("30").compareTo(folded.periodDebit));
        assertEquals(0, bd("7").compareTo(folded.periodCredit));
        assertEquals(0, bd("30").compareTo(folded.ytdDebit));
        assertEquals(0, bd("7").compareTo(folded.ytdCredit));
    }

    /**
     * 规则五（简化）：对展开后的「本期合计」行，全部科目本期借方合计应等于贷方合计。
     * 这里用两条分录对称构造。
     */
    @Test
    void trialBalance_periodDebitEqualsPeriodCredit_onExpandedRows() {
        var cash = foldOneMonth(SubjectDirectionEnum.DEBIT.getValue(),
                "0", "0", "100", "0", "100", "0");
        var capital = foldOneMonth(SubjectDirectionEnum.CREDIT.getValue(),
                "0", "0", "0", "100", "0", "100");

        List<StatementGeneralLedgerItem> rows = new java.util.ArrayList<>();
        rows.addAll(StatementGeneralLedgerRules.expandRows("1001", "现金", "2026-08", cash, false));
        rows.addAll(StatementGeneralLedgerRules.expandRows("3101", "实收资本", "2026-08", capital, false));

        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        BigDecimal closingDebitSide = BigDecimal.ZERO;
        BigDecimal closingCreditSide = BigDecimal.ZERO;
        for (StatementGeneralLedgerItem row : rows) {
            if (StatementGeneralLedgerRules.SUMMARY_PERIOD.equals(row.getSummary())) {
                periodDebit = periodDebit.add(nz(row.getDebit()));
                periodCredit = periodCredit.add(nz(row.getCredit()));
            }
            if (StatementGeneralLedgerRules.SUMMARY_YTD.equals(row.getSummary())
                    || StatementGeneralLedgerRules.SUMMARY_PERIOD.equals(row.getSummary())) {
                // 用本期行重算后的余额方向做余额试算：借方余额合计 vs 贷方余额合计
            }
            if (StatementGeneralLedgerRules.SUMMARY_PERIOD.equals(row.getSummary())) {
                if (StatementGeneralLedgerRules.DIR_DEBIT.equals(row.getDirection())) {
                    closingDebitSide = closingDebitSide.add(nz(row.getBalance()));
                } else if (StatementGeneralLedgerRules.DIR_CREDIT.equals(row.getDirection())) {
                    closingCreditSide = closingCreditSide.add(nz(row.getBalance()));
                }
            }
        }
        assertEquals(0, periodDebit.compareTo(periodCredit), "本期借贷发生额应平衡");
        assertEquals(0, closingDebitSide.compareTo(closingCreditSide), "本期后余额借贷应平衡");
        assertTrue(periodDebit.compareTo(BigDecimal.ZERO) > 0);
    }

    private static StatementGeneralLedgerRules.FoldedBalance foldOneMonth(
            String direction,
            String openD, String openC,
            String curD, String curC,
            String closeD, String closeC) {
        StatementSubjectBalance row = StatementSubjectBalance.builder()
                .yearPeriod("2026-08")
                .openingBalanceDebit(bd(openD))
                .openingBalanceCredit(bd(openC))
                .currentPeriodDebit(bd(curD))
                .currentPeriodCredit(bd(curC))
                .yearToDateDebit(bd(curD))
                .yearToDateCredit(bd(curC))
                .closingBalanceDebit(bd(closeD))
                .closingBalanceCredit(bd(closeC))
                .direction(direction)
                .build();
        return StatementGeneralLedgerRules.fold(List.of(row), direction);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
