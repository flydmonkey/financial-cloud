package com.jinbooks.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简介说明: 费用视图类
 *
 * @author wuyan
 * {@code @date} 2025/05/05 14:03:57
 * {@code @version} 1.0
 */

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
