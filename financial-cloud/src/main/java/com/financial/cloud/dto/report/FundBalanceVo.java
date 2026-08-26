package com.financial.cloud.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简介说明: 资金余额仪表盘视图类
 *
 * @author wuyan
 * {@code @date} 2025/05/05 14:03:57
 * {@code @version} 1.0
 */

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundBalanceVo {

    /**
     * 总余额：各资金账户的余额总计
     */
    private BigDecimal balance;

    /**
     * 各资金账户的余额
     */
    private List<BaseValue<BigDecimal>> subjectBalance;

    /**
     * 资金收入
     */
    private BigDecimal incomeFunds;

    /**
     * 资金支出
     */
    private BigDecimal payoutFunds;

    /**
     * 资金净收入：incomeFunds-payoutFunds
     */
    private BigDecimal netIncomeFunds;
}
