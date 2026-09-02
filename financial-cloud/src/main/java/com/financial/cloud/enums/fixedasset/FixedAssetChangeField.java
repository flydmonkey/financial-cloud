package com.financial.cloud.enums.fixedasset;

import lombok.Getter;

@Getter
public enum FixedAssetChangeField {
    NAME("name", "资产名称", false),
    DEPT_ID("deptId", "使用部门", false),
    LOCATION("location", "存放地点", false),
    SPEC("spec", "规格型号", false),
    QUANTITY("quantity", "数量", false),
    USER_ID("userId", "使用人", false),
    STATUS("status", "状态", false),
    ORIGINAL_VALUE("originalValue", "原值", true),
    RESIDUAL_RATE("residualRate", "净残值率", true),
    USEFUL_LIFE_MONTHS("usefulLifeMonths", "预计使用期数", true),
    EXPECTED_TOTAL_WORK("expectedTotalWork", "预计总工作量", true),
    DEPRECIATION_METHOD("depreciationMethod", "折旧方法", true),
    IMPAIRMENT("impairment", "减值准备", true);

    private final String code;
    private final String label;
    private final boolean calcField;

    FixedAssetChangeField(String code, String label, boolean calcField) {
        this.code = code;
        this.label = label;
        this.calcField = calcField;
    }

    public static FixedAssetChangeField fromCode(String code) {
        for (FixedAssetChangeField f : values()) {
            if (f.code.equals(code)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown change field: " + code);
    }
}
