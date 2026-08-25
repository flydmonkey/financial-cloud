package com.jinbooks.enums;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 余额方向：借方、贷方
 */

@Getter
public enum SubjectDirectionEnum implements BaseEnum {
    DEBIT("1", "借方"),             // 借方
    CREDIT("2", "贷方");            // 贷方

    private final String value;
    private final String label;

    SubjectDirectionEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public Map<Object, Object> getMap() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (SubjectDirectionEnum subjectDirectionEnum : SubjectDirectionEnum.values()) {
            map.put(subjectDirectionEnum.value, subjectDirectionEnum.label);
        }
        return map;
    }
}
