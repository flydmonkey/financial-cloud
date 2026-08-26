package com.financial.cloud.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简介说明: 其他科目指标
 *
 * @author wuyan
 * {@code @date} 2025/05/05 14:03:57
 * {@code @version} 1.0
 */

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtherSubjectsVo {
    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 各科目余额
     */
    private List<BaseValue<BigDecimal>> subjectBalance;
}
