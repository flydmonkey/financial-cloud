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
public class StatementExpenseDetailItem {
    private String sourceId;
    private String parentId;
    private String subjectCode;
    private String subjectName;
    private Integer level;
    private Map<String, BigDecimal> amounts; // period -> amount
    private BigDecimal yearTotal;
    private List<StatementExpenseDetailItem> children;
}
