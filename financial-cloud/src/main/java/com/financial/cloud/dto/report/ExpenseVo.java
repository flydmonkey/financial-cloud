package com.financial.cloud.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseVo {

    /**
     * 总余额：费用
     */
    private BigDecimal balance;

    /**
     * 同期
     */
    private Float balanceLastYear;

    /**
     * 上期
     */
    private Float balanceLast;

    /**
     * 费用分布
     */
    private List<BaseValue<BigDecimal>> balanceList;

    /**
     * 费用占收入比
     */
    private Float balanceIncomeRatio;
    /**
     * 费用占成本比
     */
    private Float balanceCostRatio;
}
