package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubjectCodeCompatTest {

    @Test
    void modernCodeMapsLegacyPayrollCodes() {
        assertEquals("2211.01", SubjectCodeCompat.modernCode("221101"));
        assertEquals("2221.14", SubjectCodeCompat.modernCode("222114"));
    }

    @Test
    void resolveFromMapPrefersExplicitTemplateCode() {
        Map<String, String> map = Map.of("2211.01", "wage");
        assertEquals("wage", SubjectCodeCompat.resolveFromMap(map, "221101"));
    }

    @Test
    void carryForwardAliasesResolveSmallBusinessCodes() {
        assertTrue(SubjectCodeCompat.carryForwardSubjectCodes("6001").contains("5001"));
        assertTrue(SubjectCodeCompat.carryForwardSubjectCodes("4103").contains("3103"));
        assertTrue(SubjectCodeCompat.lookupCandidates("4001").contains("3001"));
        Map<String, String> map = Map.of("3103", "profit");
        assertEquals("profit", SubjectCodeCompat.resolveFromMap(map, "4103"));
        Map<String, String> capital = Map.of("3001", "paid-in");
        assertEquals("paid-in", SubjectCodeCompat.resolveFromMap(capital, "4001"));
    }

    @Test
    void expandLookupCodesIncludesAliases() {
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(List.of("4001", "6001"));
        assertTrue(codes.contains("4001"));
        assertTrue(codes.contains("3001"));
        assertTrue(codes.contains("6001"));
        assertTrue(codes.contains("5001"));
        assertTrue(SubjectCodeCompat.lookupCandidates("1131").contains("1122"));
    }

    @Test
    void incomeRuleMatchesVoucherSubject_resolvesSmallBusinessAliases() {
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("6001", "5001"));
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("660201", "5602"));
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("680101", "5801"));
        assertFalse(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("6001", "5602"));
    }

    @Test
    void incomeRuleMatchesVoucherSubject_rollsUpDotAndLegacyChildSubjects() {
        // 薪资计提等自动新增的子科目（5602.07 管理费用-工资）必须归集到利润表规则 5602
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("5602", "5602.07"));
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("5602", "560207"));
        // 企业准则别名规则的子科目同样归集
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("6602", "5602.07"));
        assertTrue(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("6001", "5001.01"));
        // 不同科目不误判：5602.07 不得命中 5601 / 5603
        assertFalse(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("5601", "5602.07"));
        assertFalse(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("5603", "5602.07"));
        // 同级科目互不命中
        assertFalse(SubjectCodeCompat.incomeRuleMatchesVoucherSubject("5602", "5601.01"));
    }

    @Test
    void carryForwardSubjectCodes_includesXiaorenCostAlias() {
        assertTrue(SubjectCodeCompat.carryForwardSubjectCodes("6401").contains("5401"));
        assertTrue(SubjectCodeCompat.lookupCandidates("6401").contains("5401"));
    }

    @Test
    void carryForward_smallBusinessOnlyFixtureResolvesFromEnterpriseTemplateCodes() {
        // 模拟账套仅有小企业科目余额行：结转模板仍发企业准则编码时，候选集必须命中 5xxx/3xxx
        Map<String, BigDecimal> smallBusinessBalances = Map.of(
                "5001", new BigDecimal("1000"),
                "5602", new BigDecimal("200"),
                "3103", BigDecimal.ZERO
        );
        String revenueTemplate = "6001";
        boolean hit = SubjectCodeCompat.carryForwardSubjectCodes(revenueTemplate).stream()
                .anyMatch(smallBusinessBalances::containsKey);
        assertTrue(hit, "小企业账套应能从企业模板编码解析到 5001");

        String profitTemplate = "4103";
        assertTrue(SubjectCodeCompat.carryForwardSubjectCodes(profitTemplate).stream()
                .anyMatch(smallBusinessBalances::containsKey));
    }
}
