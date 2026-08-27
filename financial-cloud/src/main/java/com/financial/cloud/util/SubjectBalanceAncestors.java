package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 科目余额父子同步：解析 idPath，忽略前导 {@code /} 产生的空段，避免匹配 source_id='' 的脏行。
 */
public final class SubjectBalanceAncestors {

    private SubjectBalanceAncestors() {
    }

    public static List<String> sourceIdsFromIdPath(String idPath) {
        if (StringUtils.isBlank(idPath)) {
            return List.of();
        }
        return Arrays.stream(idPath.split("/"))
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    public static List<String> ancestorSourceIds(String idPath, String selfSourceId) {
        return sourceIdsFromIdPath(idPath).stream()
                .filter(id -> !id.equals(selfSourceId))
                .toList();
    }
}
