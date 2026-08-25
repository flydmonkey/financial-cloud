package com.jinbooks.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简介说明: 净利润视图类
 *
 * @author wuyan
 * {@code @date} 2025/05/05 14:03:57
 * {@code @version} 1.0
 */

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
