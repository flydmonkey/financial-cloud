package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 报表行次下钻结果：某一行报表数字由哪些科目、按什么规则构成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementDrillVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报表类型：balance_sheet / income
     */
    private String type;

    /**
     * 报表行次编码
     */
    private String itemCode;

    /**
     * 账期（yyyy-MM，区间报表为期末月）
     */
    private String yearPeriod;

    /**
     * 行次合计金额（带符号，与报表数字同口径）
     */
    private BigDecimal total;

    /**
     * 构成该行的科目明细
     */
    private List<SubjectLine> subjects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectLine implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String subjectCode;

        private String subjectName;

        /**
         * 科目方向（借/贷），利润表行无快照时为空
         */
        private String direction;

        /**
         * 取数规则（BALANCE / DEBIT_BALANCE / CREDIT_BALANCE / DEBIT_AMOUNT ...）
         */
        private String rule;

        /**
         * 计算符号（+ / -）
         */
        private String symbol;

        /**
         * 借方金额（余额表为借方余额，利润表为借方发生额）
         */
        private BigDecimal debit;

        /**
         * 贷方金额（余额表为贷方余额，利润表为贷方发生额）
         */
        private BigDecimal credit;

        /**
         * 该行对行次的带符号贡献金额
         */
        private BigDecimal amount;
    }
}
