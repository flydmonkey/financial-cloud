package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectBalanceTrialBalanceTest {

    @Test
    void trialBalanceSumsVoucherLeafRowsOnly() {
        StatementSubjectBalance parent = row("p1", null, "1002", "10", "0", "y");
        StatementSubjectBalance child = row("c1", "p1", "1002.01", "10", "0", "y");
        StatementSubjectBalance opening = row("o1", null, "3001", "100", "0", "n");
        StatementSubjectBalance other = row("c2", "p2", "5001", "0", "80", "y");

        List<StatementSubjectBalance> rows = List.of(parent, child, opening, other);
        // 仅末级且 isVoucher=y：child(10) + other(0)；排除父级汇总行与期初非凭证行
        assertEquals(new BigDecimal("10"), SubjectBalanceTrialBalance.sumCurrentPeriodDebit(rows));
        assertEquals(new BigDecimal("80"), SubjectBalanceTrialBalance.sumCurrentPeriodCredit(rows));
    }

    private static StatementSubjectBalance row(
            String sourceId, String parentId, String code, String debit, String credit, String isVoucher) {
        StatementSubjectBalance row = new StatementSubjectBalance();
        row.setSourceId(sourceId);
        row.setParentId(parentId);
        row.setSubjectCode(code);
        row.setCurrentPeriodDebit(new BigDecimal(debit));
        row.setCurrentPeriodCredit(new BigDecimal(credit));
        row.setIsVoucher(isVoucher);
        return row;
    }
}
