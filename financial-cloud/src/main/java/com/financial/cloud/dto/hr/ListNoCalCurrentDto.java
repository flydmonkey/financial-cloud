package com.financial.cloud.dto.hr;

import lombok.Data;

import java.time.YearMonth;

@Data
public class ListNoCalCurrentDto {
    YearMonth belongDate;

    String bookId;
}
