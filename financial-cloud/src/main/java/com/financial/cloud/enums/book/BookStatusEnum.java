package com.financial.cloud.enums.book;

import lombok.Getter;

/**
 * 账套状态：0 禁用，1 启用，2 封存（归档只读，禁止一切写操作）。
 */
@Getter
public enum BookStatusEnum {

    DISABLED(0, "禁用"),
    ACTIVE(1, "启用"),
    SEALED(2, "封存");

    private final Integer value;
    private final String label;

    BookStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static boolean isSealed(Integer status) {
        return SEALED.getValue().equals(status);
    }
}
