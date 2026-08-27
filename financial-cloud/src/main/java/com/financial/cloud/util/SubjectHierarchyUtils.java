package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Resolve parent subject codes for both dot-separated (1012.01) and legacy fixed-length codes.
 */
public final class SubjectHierarchyUtils {

    private SubjectHierarchyUtils() {
    }

    public static String resolveParentCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        if (code.contains(".")) {
            int dot = code.lastIndexOf('.');
            if (dot <= 0) {
                return null;
            }
            return code.substring(0, dot);
        }
        return switch (code.length()) {
            case 6 -> code.substring(0, 4);
            case 8 -> code.substring(0, 6);
            case 12 -> code.substring(0, 8);
            default -> null;
        };
    }
}
