package com.financial.cloud.dto.workspace;

import lombok.Data;

import java.util.List;

@Data
public class BooksPackBatchRequest {
    private List<String> bookIds;
    private String yearPeriod;
    private boolean includeVoucherList = true;
}
