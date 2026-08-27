package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolve subject codes across legacy fixed-length sub-codes and new dot-separated codes.
 */
public final class SubjectCodeCompat {

    private static final Map<String, String> LEGACY_TO_MODERN = Map.ofEntries(
            Map.entry("101201", "1012.01"),
            Map.entry("122102", "1221"),
            Map.entry("221101", "2211.01"),
            Map.entry("221103", "2211.04"),
            Map.entry("222114", "2221.14"),
            Map.entry("224101", "2241"),
            Map.entry("660222", "4002")
    );

    /** 企业准则模板科目 → 小企业准则等价科目（结转、报表规则共用） */
    private static final Map<String, String> CARRY_FORWARD_ALIASES = Map.ofEntries(
            Map.entry("6001", "5001"),
            Map.entry("6301", "5051"),
            Map.entry("6051", "5051"),
            Map.entry("6401", "5401"),
            Map.entry("6405", "5403"),
            Map.entry("6601", "5601"),
            Map.entry("6602", "5602"),
            Map.entry("6603", "5603"),
            Map.entry("6711", "5711"),
            Map.entry("6801", "5801"),
            Map.entry("4001", "3001"),
            Map.entry("4002", "3002"),
            Map.entry("4103", "3103"),
            Map.entry("4104", "3104"),
            Map.entry("410406", "3104.02")
    );

    private SubjectCodeCompat() {
    }

    public static String modernCode(String code) {
        if (StringUtils.isBlank(code)) {
            return code;
        }
        return LEGACY_TO_MODERN.getOrDefault(code, code);
    }

    public static Set<String> lookupCandidates(String code) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(code)) {
            candidates.add(code);
            String modern = LEGACY_TO_MODERN.get(code);
            if (modern != null) {
                candidates.add(modern);
            }
            String alias = CARRY_FORWARD_ALIASES.get(code);
            if (alias != null) {
                candidates.add(alias);
            }
            String derived = deriveDotCode(code);
            if (derived != null) {
                candidates.add(derived);
            }
        }
        return candidates;
    }

    /** 结转损益模板科目编码候选（模板编码优先，再尝试准则别名） */
    public static java.util.List<String> carryForwardSubjectCodes(String templateCode) {
        return new java.util.ArrayList<>(lookupCandidates(templateCode));
    }

    /** 报表规则绑定的科目编码及其准则别名（用于 IN 查询与余额映射） */
    public static Set<String> expandLookupCodes(java.util.Collection<String> codes) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                expanded.addAll(lookupCandidates(code));
            }
        }
        return expanded;
    }

    public static <T> T resolveFromMap(Map<String, T> map, String code) {
        for (String candidate : lookupCandidates(code)) {
            T value = map.get(candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static boolean mapContains(Map<String, ?> map, String code) {
        return resolveFromMap(map, code) != null;
    }

    /**
     * Convert legacy 6-digit sub-codes such as 221101 -> 2211.01 when parent exists.
     */
    static String deriveDotCode(String code) {
        if (StringUtils.isBlank(code) || code.contains(".") || code.length() != 6) {
            return null;
        }
        String parent = code.substring(0, 4);
        String suffix = code.substring(4);
        if (suffix.chars().allMatch(Character::isDigit)) {
            return parent + "." + suffix;
        }
        return null;
    }
}
