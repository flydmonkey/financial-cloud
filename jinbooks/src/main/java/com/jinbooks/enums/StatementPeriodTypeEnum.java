package com.jinbooks.enums;

import lombok.Getter;

/**
 * 报表查询类型枚举类
 */

@Getter
public enum StatementPeriodTypeEnum {
    YEAR("year"),                       // 年度
    MONTH("month"),                     // 月度
    QUARTER("quarter"),                 // 季度
    HALF_YEAR("halfYear"),              // 半年
    BETWEEN_MONTH("between");           // 月份区间

    private final String value;

    StatementPeriodTypeEnum(String value) {
        this.value = value;
    }
}
