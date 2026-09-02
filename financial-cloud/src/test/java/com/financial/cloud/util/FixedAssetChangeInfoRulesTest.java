package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedAssetChangeInfoRulesTest {

    @Test
    void formatItem_arrowText() {
        assertEquals("原值:10000→12000",
                FixedAssetChangeInfoRules.formatItem("原值", "10000", "12000"));
    }

    @Test
    void formatOneChange_prefersItemsOverRemark() {
        assertEquals("原值:1→2；使用月数:60→48",
                FixedAssetChangeInfoRules.formatOneChange("忽略",
                        List.of("原值:1→2", "使用月数:60→48")));
    }

    @Test
    void formatOneChange_fallsBackToRemark() {
        assertEquals("资产清理",
                FixedAssetChangeInfoRules.formatOneChange("资产清理", List.of()));
    }

    @Test
    void joinAssetChanges_skipsBlank() {
        assertEquals("a；b",
                FixedAssetChangeInfoRules.joinAssetChanges(List.of("a", "", "b")));
    }
}
