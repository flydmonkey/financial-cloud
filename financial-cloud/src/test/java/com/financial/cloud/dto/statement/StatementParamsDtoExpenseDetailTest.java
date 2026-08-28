package com.financial.cloud.dto.statement;

import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementParamsDtoExpenseDetailTest {

    @Test
    void between_parsesDateRangeAndBounds() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("b1")
                .periodType("between")
                .dateRange(new String[]{"2023-01", "2023-12"})
                .build();
        dto.parse();
        assertEquals("2023-01-01", dto.getDateRangeStart());
        assertEquals("2023-12-31", dto.getDateRangeEnd());
        assertEquals(12, dto.getAllMonths().size());
    }

    @Test
    void between_rejectsMoreThan24Months() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("b1")
                .periodType("between")
                .dateRange(new String[]{"2022-01", "2024-02"})
                .build();
        assertThrows(BusinessException.class, dto::parse);
    }

    @Test
    void between_rejectsMalformedMonthAsDateRangeError() {
        StatementParamsDto dto = StatementParamsDto.builder()
                .bookId("b1")
                .periodType("between")
                .dateRange(new String[]{"2023-1", "2023-02"})
                .build();

        BusinessException exception = assertThrows(BusinessException.class, dto::parse);

        assertEquals(StatementErrorCode.DATE_RANGE_SIZE.getCode(), exception.getCode());
    }
}
