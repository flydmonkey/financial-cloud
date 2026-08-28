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
            Map.entry("410406", "3104.02"),
            Map.entry("1131", "1122"),
            Map.entry("1151", "1123"),
            Map.entry("2121", "2202"),
            Map.entry("2131", "2203")
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
     * 利润表模板科目 → 账套科目（660201→5602，6001→5001）。
     */
    public static String mapIncomeRuleSubject(String subjectCode) {
        if (StringUtils.isBlank(subjectCode)) {
            return subjectCode;
        }
        String alias = CARRY_FORWARD_ALIASES.get(subjectCode);
        if (alias != null) {
            return alias;
        }
        if (subjectCode.startsWith("6602")) {
            return "5602";
        }
        if (subjectCode.startsWith("6601")) {
            return "5601";
        }
        if (subjectCode.startsWith("6603")) {
            return "5603";
        }
        if (subjectCode.startsWith("6801")) {
            return "5801";
        }
        if (subjectCode.startsWith("6401")) {
            return "5401";
        }
        if (subjectCode.startsWith("6402")) {
            return "5402";
        }
        if (subjectCode.startsWith("6405")) {
            return "5403";
        }
        if (subjectCode.startsWith("6051") || subjectCode.startsWith("6301")) {
            return "5051";
        }
        return subjectCode;
    }

    /**
     * 利润表规则科目是否与凭证分录科目匹配（含准则别名；子科目规则 660201 不重复匹配父科目 5602）。
     */
    public static boolean incomeRuleMatchesVoucherSubject(String ruleSubjectCode, String voucherSubjectCode) {
        if (StringUtils.isBlank(ruleSubjectCode) || StringUtils.isBlank(voucherSubjectCode)) {
            return false;
        }
        if (StringUtils.equalsIgnoreCase(ruleSubjectCode, voucherSubjectCode)) {
            return true;
        }
        if (matchesViaLookupCandidates(ruleSubjectCode, voucherSubjectCode)) {
            return true;
        }
        if (matchesViaLookupCandidates(voucherSubjectCode, ruleSubjectCode)) {
            return true;
        }
        return StringUtils.equalsIgnoreCase(
                mapIncomeRuleSubject(ruleSubjectCode), voucherSubjectCode);
    }

    private static boolean matchesViaLookupCandidates(String left, String right) {
        for (String candidate : lookupCandidates(left)) {
            if (StringUtils.equalsIgnoreCase(candidate, right)) {
                return true;
            }
        }
        return false;
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
