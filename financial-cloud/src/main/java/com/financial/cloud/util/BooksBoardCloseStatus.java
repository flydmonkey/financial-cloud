package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * 代账工作台：账套当前开放账期 C 相对关注月 F 的结账启发式。
 * C &gt; F → 已结；C == F → 未结；C &lt; F → 落后。
 */
public enum BooksBoardCloseStatus {
    CLOSED,
    OPEN,
    BEHIND,
    UNKNOWN;

    public static BooksBoardCloseStatus resolve(String currentTerm, String focusPeriod) {
        YearMonth current = parse(currentTerm);
        YearMonth focus = parse(focusPeriod);
        if (current == null || focus == null) {
            return UNKNOWN;
        }
        int cmp = current.compareTo(focus);
        if (cmp > 0) {
            return CLOSED;
        }
        if (cmp == 0) {
            return OPEN;
        }
        return BEHIND;
    }

    private static YearMonth parse(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
