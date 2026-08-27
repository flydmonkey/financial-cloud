package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubjectHierarchyUtilsTest {

    @Test
    void resolveParentCode_supportsDotSeparatedCodes() {
        assertNull(SubjectHierarchyUtils.resolveParentCode("1001"));
        assertEquals("1012", SubjectHierarchyUtils.resolveParentCode("1012.01"));
        assertEquals("1101.01", SubjectHierarchyUtils.resolveParentCode("1101.01.02"));
    }

    @Test
    void resolveParentCode_supportsLegacyFixedLengthCodes() {
        assertEquals("1001", SubjectHierarchyUtils.resolveParentCode("100101"));
        assertEquals("100101", SubjectHierarchyUtils.resolveParentCode("10010101"));
    }
}
