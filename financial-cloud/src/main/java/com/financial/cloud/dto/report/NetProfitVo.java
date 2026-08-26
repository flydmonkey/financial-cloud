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
public class NetProfitVo {

    /**
     * 总余额：净利润
     */
    private BigDecimal balance;

    /**
     * 同期净利润
     */
    private Float balanceLastYear;

    /**
     * 上期净利润
     */
    private Float balanceLast;

    /**
     * 净利润率
     */
    private Float balanceRatio;

    /**
     * 同期净利润率
     */
    private Float balanceRatioLastYear;

    /**
     * 上期净利润率
     */
    private Float balanceRatioLast;

    /**
     * 近期变动趋势-净利润
     */
    private List<BaseValue<BigDecimal>> balanceList;

    /**
     * 近期变动趋势-净利润率
     */
    private List<BaseValue<Float>> balanceRatioList;
}
