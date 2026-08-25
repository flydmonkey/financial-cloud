package com.jinbooks.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简介说明: 税金及附加视图类
 *
 * @author wuyan
 * {@code @date} 2025/05/27 14:03:57
 * {@code @version} 1.0
 */

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTaxVo {

    /**
     * 预计应缴税额
     */
    private BigDecimal balance;

    /**
     * 税负率
     */
    private Float ratio;

    /**
     * 近期变动趋势-税负
     */
    private List<BaseValue<BigDecimal>> tax;

    /**
     * 近期变动趋势-税负率
     */
    private List<BaseValue<BigDecimal>> taxRatio;

}
