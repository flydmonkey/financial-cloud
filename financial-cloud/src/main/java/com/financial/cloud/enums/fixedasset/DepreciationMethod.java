package com.financial.cloud.enums.fixedasset;

import lombok.Getter;

@Getter
public enum DepreciationMethod {
    STRAIGHT_LINE("平均年限法"),
    UNITS_OF_PRODUCTION("工作量法"),
    DOUBLE_DECLINING("双倍余额递减法"),
    SUM_OF_YEARS("年数总和法"),
    NONE("不计提折旧");

    private final String label;

    DepreciationMethod(String label) {
        this.label = label;
    }

    public static DepreciationMethod from(String value) {
        if (value == null) {
            return STRAIGHT_LINE;
        }
        for (DepreciationMethod method : values()) {
            if (method.name().equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unsupported depreciation method: " + value);
    }

    public boolean isDepreciable() {
        return this != NONE;
    }

    public boolean isAccelerated() {
        return this == DOUBLE_DECLINING || this == SUM_OF_YEARS;
    }
}
