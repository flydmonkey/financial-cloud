package com.financial.cloud.dto.journal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 银行对账 / 余额调节表：企业日记账余额 vs 银行对账单余额，未达账项调节。
 */
public class JournalReconciliationDtos {

    @Data
    public static class StatementDto {
        private String accId;
        private String yearPeriod;
        private BigDecimal statementBalance;
        private String remark;
    }

    @Data
    public static class MarkDto {
        private List<String> entryIds;
        private Boolean reconciled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String accId;
        private String accName;
        private String yearPeriod;

        /** 企业日记账期末余额（流水净额累计） */
        private BigDecimal bookBalance;
        /** 银行对账单期末余额（未登记为 null） */
        private BigDecimal statementBalance;
        private String remark;

        /** 企业已收银行未收（未对账收入合计） */
        private BigDecimal unreconciledIncome;
        /** 企业已付银行未付（未对账支出合计） */
        private BigDecimal unreconciledExpenditure;
        /** 调节后银行余额 = 对账单余额 + 未收 - 未付 */
        private BigDecimal adjustedStatement;
        /** 差额 = 企业账面余额 - 调节后银行余额（0 表示调节平衡） */
        private BigDecimal difference;

        /** 截至期末的流水（含对账标记） */
        private List<EntryRow> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryRow implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private String tradeDate;
        private String summary;
        private String direction;
        private BigDecimal income;
        private BigDecimal expenditure;
        private BigDecimal balance;
        private Boolean reconciled;
        /** 方向为 o 的期初流水，默认视为已对账、不计未达账项 */
        private Boolean opening;
    }
}
