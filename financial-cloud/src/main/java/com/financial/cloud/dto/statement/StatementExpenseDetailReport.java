package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementExpenseDetailReport {
    private List<String> periods;
    private String yearLabel;
    private List<StatementExpenseDetailItem> items;
    private Map<String, BigDecimal> totals;
    private List<ExpenseReconciliationWarning> reconciliationWarnings;
}
