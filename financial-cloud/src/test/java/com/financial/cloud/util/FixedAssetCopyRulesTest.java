package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedAssetCopyRulesTest {

    @Test
    void nextCopyCode_appendsSuffix() {
        assertEquals("FA01-副本", FixedAssetCopyRules.nextCopyCode("FA01", code -> false));
    }

    @Test
    void nextCopyCode_incrementsWhenConflict() {
        Set<String> exists = new HashSet<>();
        exists.add("FA01-副本");
        exists.add("FA01-副本2");
        assertEquals("FA01-副本3", FixedAssetCopyRules.nextCopyCode("FA01", exists::contains));
    }
}
