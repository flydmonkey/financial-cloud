package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.common.YesNoEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 科目余额表试算平衡：仅汇总末级科目，避免父级汇总行重复计入本期发生额。
 */
public final class SubjectBalanceTrialBalance {

    private SubjectBalanceTrialBalance() {
    }

    public static List<StatementSubjectBalance> leafRows(List<StatementSubjectBalance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return List.of();
        }
        Set<String> parentSourceIds = new HashSet<>();
        for (StatementSubjectBalance row : rows) {
            if (StringUtils.isNotBlank(row.getParentId())) {
                parentSourceIds.add(row.getParentId());
            }
        }
        return rows.stream()
                .filter(row -> StringUtils.isNotBlank(row.getSourceId()))
                .filter(row -> !parentSourceIds.contains(row.getSourceId()))
                .collect(Collectors.toList());
    }

    public static List<StatementSubjectBalance> voucherLeafRows(List<StatementSubjectBalance> rows) {
        return leafRows(rows).stream()
                .filter(row -> YesNoEnum.y.name().equals(row.getIsVoucher()))
                .collect(Collectors.toList());
    }

    public static BigDecimal sumCurrentPeriodDebit(List<StatementSubjectBalance> rows) {
        return voucherLeafRows(rows).stream()
                .map(StatementSubjectBalance::getCurrentPeriodDebit)
                .map(SubjectBalanceTrialBalance::defaultZero)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumCurrentPeriodCredit(List<StatementSubjectBalance> rows) {
        return voucherLeafRows(rows).stream()
                .map(StatementSubjectBalance::getCurrentPeriodCredit)
                .map(SubjectBalanceTrialBalance::defaultZero)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
