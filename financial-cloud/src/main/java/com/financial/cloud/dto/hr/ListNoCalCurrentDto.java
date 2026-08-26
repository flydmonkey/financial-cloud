package com.financial.cloud.dto.hr;

import lombok.Data;

import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/18 14:45
 */

@Data
public class ListNoCalCurrentDto {
    YearMonth belongDate;

    String bookId;
}
