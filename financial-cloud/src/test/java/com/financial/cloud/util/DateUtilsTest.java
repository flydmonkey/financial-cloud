package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilsTest {

    @Test
    void format_usesShanghaiCalendarMonth_forGmtPlus8Midnight() {
        // Jackson @JsonFormat(timezone=GMT+8) for "2026-01-01" → Instant 2025-12-31T16:00:00Z
        Date voucherDate = Date.from(
                LocalDate.of(2026, 1, 1)
                        .atStartOfDay(ZoneId.of("Asia/Shanghai"))
                        .toInstant());

        assertEquals("2026-01", DateUtils.format(voucherDate, DateUtils.FORMAT_DATE_YYYY_MM));
        assertEquals("2026-01-01", DateUtils.format(voucherDate, DateUtils.FORMAT_DATE_YYYY_MM_DD));
    }

    @Test
    void format_midMonthUnaffectedByZone() {
        Date midMonth = Date.from(
                LocalDate.of(2026, 1, 15)
                        .atStartOfDay(ZoneId.of("Asia/Shanghai"))
                        .toInstant());
        assertEquals("2026-01", DateUtils.format(midMonth, DateUtils.FORMAT_DATE_YYYY_MM));
    }
}
