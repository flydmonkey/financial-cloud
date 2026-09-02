package com.financial.cloud.enums.fixedasset;

import lombok.Getter;

@Getter
public enum FixedAssetStatus {
    IN_USE("正常使用"),
    SUSPENDED("暂停计提"),
    DISPOSED("已清理");

    private final String label;

    FixedAssetStatus(String label) {
        this.label = label;
    }

    public static FixedAssetStatus from(String value) {
        if (value == null) {
            return IN_USE;
        }
        for (FixedAssetStatus s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return IN_USE;
    }
}
