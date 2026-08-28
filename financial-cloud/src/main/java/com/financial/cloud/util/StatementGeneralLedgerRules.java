package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementGeneralLedgerItem;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 总账纯函数：期间码、区间折叠、隐藏规则、三行展开。
 */
public final class StatementGeneralLedgerRules {

    public static final String SUMMARY_OPENING = "期初余额";
    public static final String SUMMARY_PERIOD = "本期合计";
    public static final String SUMMARY_YTD = "本年累计";

    public static final String DIR_DEBIT = "借";
    public static final String DIR_CREDIT = "贷";
    public static final String DIR_FLAT = "平";

    private StatementGeneralLedgerRules() {
    }

    public static String periodCode(String yearMonth) {
        if (StringUtils.isBlank(yearMonth)) {
            return "";
        }
        return yearMonth.replace("-", "");
    }

    public static FoldedBalance fold(List<StatementSubjectBalance> monthsOrdered, String subjectDirection) {
        FoldedBalance folded = new FoldedBalance();
        folded.direction = subjectDirection;
        if (monthsOrdered == null || monthsOrdered.isEmpty()) {
            return folded;
        }
        StatementSubjectBalance first = monthsOrdered.get(0);
        StatementSubjectBalance last = monthsOrdered.get(monthsOrdered.size() - 1);
        folded.openingDebit = nz(first.getOpeningBalanceDebit());
        folded.openingCredit = nz(first.getOpeningBalanceCredit());
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        for (StatementSubjectBalance row : monthsOrdered) {
            periodDebit = periodDebit.add(nz(row.getCurrentPeriodDebit()));
            periodCredit = periodCredit.add(nz(row.getCurrentPeriodCredit()));
        }
        folded.periodDebit = periodDebit;
        folded.periodCredit = periodCredit;
        folded.ytdDebit = nz(last.getYearToDateDebit());
        folded.ytdCredit = nz(last.getYearToDateCredit());
        folded.closingDebit = nz(last.getClosingBalanceDebit());
        folded.closingCredit = nz(last.getClosingBalanceCredit());
        return folded;
    }

    public static boolean shouldHideGroup(
            FoldedBalance folded, boolean hideZeroBalance, boolean hideNoActivityAndZeroBalance) {
        if (folded == null) {
            return true;
        }
        BigDecimal closing = signedBalance(folded.closingDebit, folded.closingCredit, folded.direction);
        boolean zeroClosing = closing.compareTo(BigDecimal.ZERO) == 0;
        boolean noActivity = folded.periodDebit.compareTo(BigDecimal.ZERO) == 0
                && folded.periodCredit.compareTo(BigDecimal.ZERO) == 0;
        if (hideZeroBalance && zeroClosing) {
            return true;
        }
        if (hideNoActivityAndZeroBalance && noActivity && zeroClosing) {
            return true;
        }
        return false;
    }

    public static List<StatementGeneralLedgerItem> expandRows(
            String subjectCode,
            String subjectName,
            String periodYyyyMm,
            FoldedBalance folded,
            boolean hidePeriodRowsWhenNoActivity) {
        List<StatementGeneralLedgerItem> rows = new ArrayList<>();
        if (folded == null) {
            return rows;
        }
        String period = periodCode(periodYyyyMm);
        String groupKey = subjectCode;
        boolean noActivity = folded.periodDebit.compareTo(BigDecimal.ZERO) == 0
                && folded.periodCredit.compareTo(BigDecimal.ZERO) == 0;
        boolean onlyOpening = hidePeriodRowsWhenNoActivity && noActivity;
        int span = onlyOpening ? 1 : 3;

        rows.add(openingRow(subjectCode, subjectName, period, groupKey, span, folded));
        if (!onlyOpening) {
            rows.add(periodRow(subjectCode, subjectName, period, groupKey, folded));
            rows.add(ytdRow(subjectCode, subjectName, period, groupKey, folded));
        }
        return rows;
    }

    private static StatementGeneralLedgerItem openingRow(
            String code, String name, String period, String groupKey, int span, FoldedBalance folded) {
        BigDecimal bal = signedBalance(folded.openingDebit, folded.openingCredit, folded.direction);
        return StatementGeneralLedgerItem.builder()
                .subjectCode(code)
                .subjectName(name)
                .period(period)
                .summary(SUMMARY_OPENING)
                .debit(null)
                .credit(null)
                .direction(displayDirection(bal, folded.direction))
                .balance(isZero(bal) ? null : bal.abs())
                .groupKey(groupKey)
                .rowSpan(span)
                .build();
    }

    private static StatementGeneralLedgerItem periodRow(
            String code, String name, String period, String groupKey, FoldedBalance folded) {
        BigDecimal bal = signedClosingFromOpeningAndPeriod(folded);
        return StatementGeneralLedgerItem.builder()
                .subjectCode(code)
                .subjectName(name)
                .period(period)
                .summary(SUMMARY_PERIOD)
                .debit(zeroToNull(folded.periodDebit))
                .credit(zeroToNull(folded.periodCredit))
                .direction(displayDirection(bal, folded.direction))
                .balance(isZero(bal) ? null : bal.abs())
                .groupKey(groupKey)
                .rowSpan(0)
                .build();
    }

    private static StatementGeneralLedgerItem ytdRow(
            String code, String name, String period, String groupKey, FoldedBalance folded) {
        // YTD 行余额与期末一致
        BigDecimal closing = signedBalance(folded.closingDebit, folded.closingCredit, folded.direction);
        return StatementGeneralLedgerItem.builder()
                .subjectCode(code)
                .subjectName(name)
                .period(period)
                .summary(SUMMARY_YTD)
                .debit(zeroToNull(folded.ytdDebit))
                .credit(zeroToNull(folded.ytdCredit))
                .direction(displayDirection(closing, folded.direction))
                .balance(isZero(closing) ? null : closing.abs())
                .groupKey(groupKey)
                .rowSpan(0)
                .build();
    }

    /** 由分项构造 FoldedBalance，并按规则四计算期末 */
    public static FoldedBalance fromParts(
            BigDecimal openingDebit, BigDecimal openingCredit,
            BigDecimal periodDebit, BigDecimal periodCredit,
            BigDecimal ytdDebit, BigDecimal ytdCredit,
            String direction) {
        FoldedBalance folded = new FoldedBalance();
        folded.direction = direction;
        folded.openingDebit = nz(openingDebit);
        folded.openingCredit = nz(openingCredit);
        folded.periodDebit = nz(periodDebit);
        folded.periodCredit = nz(periodCredit);
        folded.ytdDebit = nz(ytdDebit);
        folded.ytdCredit = nz(ytdCredit);
        applyClosingFromOpeningAndPeriod(folded);
        return folded;
    }

    /** 规则四：期末 = 期初 ± 本期（按科目方向），写回 closingDebit/Credit */
    public static void applyClosingFromOpeningAndPeriod(FoldedBalance folded) {
        BigDecimal signed = signedClosingFromOpeningAndPeriod(folded);
        folded.closingDebit = BigDecimal.ZERO;
        folded.closingCredit = BigDecimal.ZERO;
        if (signed.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (SubjectDirectionEnum.CREDIT.getValue().equals(folded.direction)) {
            if (signed.compareTo(BigDecimal.ZERO) > 0) {
                folded.closingCredit = signed;
            } else {
                folded.closingDebit = signed.abs();
            }
        } else if (signed.compareTo(BigDecimal.ZERO) > 0) {
            folded.closingDebit = signed;
        } else {
            folded.closingCredit = signed.abs();
        }
    }

    /** 期初净额 + 本期 → 与期末勾稽用的带符号余额（借方科目借为正；贷方科目贷为正） */
    public static BigDecimal signedClosingFromOpeningAndPeriod(FoldedBalance folded) {
        BigDecimal opening = signedBalance(folded.openingDebit, folded.openingCredit, folded.direction);
        BigDecimal period = signedBalance(folded.periodDebit, folded.periodCredit, folded.direction);
        return opening.add(period);
    }

    /**
     * 规则五：对本期合计行做试算。
     */
    public static TrialBalance trialBalance(List<StatementGeneralLedgerItem> items) {
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        BigDecimal closingDebit = BigDecimal.ZERO;
        BigDecimal closingCredit = BigDecimal.ZERO;
        if (items != null) {
            for (StatementGeneralLedgerItem row : items) {
                if (row == null || !SUMMARY_PERIOD.equals(row.getSummary())) {
                    continue;
                }
                periodDebit = periodDebit.add(nz(row.getDebit()));
                periodCredit = periodCredit.add(nz(row.getCredit()));
                if (DIR_DEBIT.equals(row.getDirection())) {
                    closingDebit = closingDebit.add(nz(row.getBalance()));
                } else if (DIR_CREDIT.equals(row.getDirection())) {
                    closingCredit = closingCredit.add(nz(row.getBalance()));
                }
            }
        }
        TrialBalance tb = new TrialBalance();
        tb.periodDebitTotal = periodDebit;
        tb.periodCreditTotal = periodCredit;
        tb.closingDebitTotal = closingDebit;
        tb.closingCreditTotal = closingCredit;
        tb.periodBalanced = periodDebit.subtract(periodCredit).abs().compareTo(new BigDecimal("0.01")) <= 0;
        tb.balanceBalanced = closingDebit.subtract(closingCredit).abs().compareTo(new BigDecimal("0.01")) <= 0;
        tb.balanced = tb.periodBalanced && tb.balanceBalanced;
        return tb;
    }

    public static final class TrialBalance {
        public BigDecimal periodDebitTotal = BigDecimal.ZERO;
        public BigDecimal periodCreditTotal = BigDecimal.ZERO;
        public BigDecimal closingDebitTotal = BigDecimal.ZERO;
        public BigDecimal closingCreditTotal = BigDecimal.ZERO;
        public boolean periodBalanced;
        public boolean balanceBalanced;
        public boolean balanced;
    }

    /**
     * 按科目方向将借贷分列转为带符号净额：借方科目 借−贷；贷方科目 贷−借。
     */
    public static BigDecimal signedBalance(BigDecimal debit, BigDecimal credit, String direction) {
        BigDecimal d = nz(debit);
        BigDecimal c = nz(credit);
        if (SubjectDirectionEnum.CREDIT.getValue().equals(direction)) {
            return c.subtract(d);
        }
        return d.subtract(c);
    }

    /**
     * signed 为按科目方向计算的净额（正常余额为正）。
     * 借方科目：正→借，负→贷；贷方科目：正→贷，负→借。
     */
    public static String displayDirection(BigDecimal signed, String subjectDirection) {
        if (signed == null || signed.compareTo(BigDecimal.ZERO) == 0) {
            return DIR_FLAT;
        }
        boolean normalSide = signed.compareTo(BigDecimal.ZERO) > 0;
        if (SubjectDirectionEnum.CREDIT.getValue().equals(subjectDirection)) {
            return normalSide ? DIR_CREDIT : DIR_DEBIT;
        }
        return normalSide ? DIR_DEBIT : DIR_CREDIT;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static boolean isZero(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal zeroToNull(BigDecimal v) {
        return isZero(v) ? null : v;
    }

    public static final class FoldedBalance {
        public BigDecimal openingDebit = BigDecimal.ZERO;
        public BigDecimal openingCredit = BigDecimal.ZERO;
        public BigDecimal periodDebit = BigDecimal.ZERO;
        public BigDecimal periodCredit = BigDecimal.ZERO;
        public BigDecimal ytdDebit = BigDecimal.ZERO;
        public BigDecimal ytdCredit = BigDecimal.ZERO;
        public BigDecimal closingDebit = BigDecimal.ZERO;
        public BigDecimal closingCredit = BigDecimal.ZERO;
        /** SubjectDirectionEnum value: 1/2 */
        public String direction;
    }
}
