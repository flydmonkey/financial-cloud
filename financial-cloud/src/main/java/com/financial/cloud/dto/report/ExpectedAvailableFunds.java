package com.financial.cloud.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedAvailableFunds {

    /**
     * 预计可用资金
     */
    private BigDecimal balance;

    /**
     * 现有资金
     */
    private BigDecimal cashBalance;

    /**
     * 短期应收款
     */
    private BigDecimal accountsReceivable;

    /**
     * 短期应付款
     */
    private BigDecimal accountsPayable;

    /**
     * 现金比率
     */
    private Float cashRatio;

    /**
     * 同期现金比率
     */
    private Float cashRatioLastYear;

    /**
     * 速动比率
     */
    private Float quickRatio;

    /**
     * 同期速动比率
     */
    private Float quickRatioLastYear;
}
