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
