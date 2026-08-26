package com.financial.cloud.enums.common;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public enum YesNoEnum implements BaseEnum {
    /**
     * 是
     */
    y("是"),

    /**
     * 否
     */
    n("否");

    private final String label;

    YesNoEnum(String label) {
        this.label = label;
    }

    @Override
    public Map<Object, Object> getMap() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (YesNoEnum yn : YesNoEnum.values()) {
            map.put(yn.name(), yn.getLabel());
        }
        return map;
    }
}
