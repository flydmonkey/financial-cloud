package com.jinbooks.enums;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Map;

/**
 * 凭证审核开关状态
 * 0-关闭;1-开启
 */

@Getter
public enum OnOffEnum implements BaseEnum {
    ON(1, "开启"),
    OFF(0, "关闭");

    private final Integer code;
    private final String name;

    OnOffEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Map<Object, Object> getMap() {
        Map<Object, Object> map = Maps.newHashMap();
        for (OnOffEnum status : OnOffEnum.values()) {
            map.put(status.getCode(), status.getName());
        }
        return map;
    }
}
