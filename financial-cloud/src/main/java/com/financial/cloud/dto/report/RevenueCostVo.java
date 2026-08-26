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
public class RevenueCostVo {

    /**
     * 营业收入
     */
    private BigDecimal balance;

    /**
     * 营业成本
     */
    private BigDecimal balanceOperatingCosts;

    /**
     * 同期收入
     */
    private Float balanceLastYear;

    /**
     * 上期收入
     */
    private Float balanceLast;

    /**
     * 毛利率
     */
    private Float balanceRatio;

    /**
     * 同期成本
     */
    private Float balanceRatioLastYear;

    /**
     * 上期成本
     */
    private Float balanceRatioLast;

    /**
     * 近期变动趋势-收入
     */
    private List<BaseValue<BigDecimal>> balanceList;

    /**
     * 近期变动趋势-成本
     */
    private List<BaseValue<BigDecimal>> balanceRatioList;
}
