package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * 资产负债表取数规则：余额 / 借方余额 / 贷方余额（用于往来重分类）。
 */
public final class StatementBalanceSheetRules {

    public static final String BALANCE = "BALANCE";
    public static final String DEBIT_BALANCE = "DEBIT_BALANCE";
    public static final String CREDIT_BALANCE = "CREDIT_BALANCE";

    private StatementBalanceSheetRules() {
    }

    public static BigDecimal normalizeClosingBalance(StatementSubjectBalance subjectBalance, String rule) {
        return normalizeBalance(
                subjectBalance,
                rule,
                defaultZero(subjectBalance.getClosingBalanceDebit()),
                defaultZero(subjectBalance.getClosingBalanceCredit()));
    }

    public static BigDecimal normalizeOpeningBalance(StatementSubjectBalance subjectBalance, String rule) {
        return normalizeBalance(
                subjectBalance,
                rule,
                defaultZero(subjectBalance.getOpeningYearBalanceDebit()),
                defaultZero(subjectBalance.getOpeningYearBalanceCredit()));
    }

    private static BigDecimal normalizeBalance(
            StatementSubjectBalance subjectBalance,
            String rule,
            BigDecimal debit,
            BigDecimal credit) {
        if (StringUtils.isBlank(rule) || BALANCE.equalsIgnoreCase(rule)) {
            return normalizeByDirection(
                    subjectBalance.getDirection(),
                    debit,
                    credit,
                    defaultZero(subjectBalance.getBalance()));
        }
        if (DEBIT_BALANCE.equalsIgnoreCase(rule)) {
            return netDebitBalance(subjectBalance, debit, credit);
        }
        if (CREDIT_BALANCE.equalsIgnoreCase(rule)) {
            return netCreditBalance(subjectBalance, debit, credit);
        }
        return normalizeByDirection(
                subjectBalance.getDirection(),
                debit,
                credit,
                defaultZero(subjectBalance.getBalance()));
    }

    public static BigDecimal normalizeByDirection(String direction,
                                           BigDecimal debit,
                                           BigDecimal credit,
                                           BigDecimal fallbackBalance) {
        if (SubjectDirectionEnum.CREDIT.getValue().equals(direction)) {
            if (debit.signum() != 0 || credit.signum() != 0) {
                return credit.subtract(debit);
            }
            if (fallbackBalance.signum() < 0) {
                return fallbackBalance.negate();
            }
            return fallbackBalance;
        }
        if (debit.signum() != 0 || credit.signum() != 0) {
            return debit.subtract(credit);
        }
        return fallbackBalance;
    }

    /** 借方余额：仅保留 net debit &gt; 0 的部分（贷方余额重分类出去）。 */
    static BigDecimal netDebitBalance(StatementSubjectBalance subjectBalance, BigDecimal debit, BigDecimal credit) {
        if (debit.signum() != 0 || credit.signum() != 0) {
            BigDecimal net = debit.subtract(credit);
            return net.signum() > 0 ? net : BigDecimal.ZERO;
        }
        BigDecimal balance = defaultZero(subjectBalance.getBalance());
        if (SubjectDirectionEnum.DEBIT.getValue().equals(subjectBalance.getDirection())) {
            return balance.signum() > 0 ? balance : BigDecimal.ZERO;
        }
        return balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO;
    }

    /** 贷方余额：仅保留 net credit &gt; 0 的部分（借方余额重分类出去）。 */
    static BigDecimal netCreditBalance(StatementSubjectBalance subjectBalance, BigDecimal debit, BigDecimal credit) {
        if (debit.signum() != 0 || credit.signum() != 0) {
            BigDecimal net = credit.subtract(debit);
            return net.signum() > 0 ? net : BigDecimal.ZERO;
        }
        BigDecimal balance = defaultZero(subjectBalance.getBalance());
        if (SubjectDirectionEnum.CREDIT.getValue().equals(subjectBalance.getDirection())) {
            return balance.signum() > 0 ? balance : BigDecimal.ZERO;
        }
        return balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
