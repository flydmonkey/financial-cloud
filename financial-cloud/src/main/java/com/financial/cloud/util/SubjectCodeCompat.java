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
            String derived = deriveDotCode(code);
            if (derived != null) {
                candidates.add(derived);
            }
        }
        return candidates;
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
