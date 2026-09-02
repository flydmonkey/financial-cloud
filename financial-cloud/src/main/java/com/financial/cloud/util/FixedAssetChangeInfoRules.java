package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 折旧明细表「期间变动」摘要文案。
 */
public final class FixedAssetChangeInfoRules {

    private FixedAssetChangeInfoRules() {
    }

    public static String formatItem(String fieldLabel, String beforeValue, String afterValue) {
        String label = StringUtils.defaultIfBlank(fieldLabel, "变动");
        return label + ":" + StringUtils.defaultString(beforeValue) + "→" + StringUtils.defaultString(afterValue);
    }

    /**
     * 单张变动单：有明细则拼明细；无明细则用 remark。
     */
    public static String formatOneChange(String remark, List<String> itemTexts) {
        if (itemTexts != null && !itemTexts.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (String t : itemTexts) {
                if (StringUtils.isNotBlank(t)) {
                    parts.add(t);
                }
            }
            if (!parts.isEmpty()) {
                return String.join("；", parts);
            }
        }
        return StringUtils.trimToEmpty(remark);
    }

    /** 同一资产多张变动单摘要，用中文分号连接 */
    public static String joinAssetChanges(List<String> changeTexts) {
        if (changeTexts == null || changeTexts.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String t : changeTexts) {
            if (StringUtils.isNotBlank(t)) {
                parts.add(t.trim());
            }
        }
        return String.join("；", parts);
    }
}
