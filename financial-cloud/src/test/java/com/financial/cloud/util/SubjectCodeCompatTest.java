package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

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
    void deriveDotCodeConvertsSixDigitSubCode() {
        assertEquals("2211.01", SubjectCodeCompat.deriveDotCode("221101"));
        assertNull(SubjectCodeCompat.deriveDotCode("1001"));
    }
}
