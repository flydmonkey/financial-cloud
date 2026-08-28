package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementGeneralLedgerReport {
    @Builder.Default
    private List<StatementGeneralLedgerItem> items = new ArrayList<>();
    /** 科目组数（底栏「共 N 条」） */
    private int subjectCount;

    /** 规则五：试算是否平衡 */
    private Boolean trialBalanced;
    private Boolean periodTrialBalanced;
    private Boolean balanceTrialBalanced;
    private BigDecimal periodDebitTotal;
    private BigDecimal periodCreditTotal;
    private BigDecimal closingDebitTotal;
    private BigDecimal closingCreditTotal;

    /** 账证/账账等勾稽提示 */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
