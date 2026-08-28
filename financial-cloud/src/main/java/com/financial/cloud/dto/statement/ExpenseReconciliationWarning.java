package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReconciliationWarning {
    private String subjectCode;
    private String period;
    private BigDecimal detailAmount;
    private BigDecimal incomeAmount;
    private BigDecimal diff;
}
