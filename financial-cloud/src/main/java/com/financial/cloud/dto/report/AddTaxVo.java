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
