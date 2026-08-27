package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

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
    }
}
