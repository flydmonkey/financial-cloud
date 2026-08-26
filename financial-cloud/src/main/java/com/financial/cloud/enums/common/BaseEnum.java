package com.financial.cloud.enums.common;

import java.util.Collections;
import java.util.Map;

public interface BaseEnum {
    Map<Object, Object> getMap();

    // 用于注解默认值判断
    abstract class Default implements BaseEnum {
        public Map<Object, Object> getMap() { return Collections.emptyMap(); }
    }
}
