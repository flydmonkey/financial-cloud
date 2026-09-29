package com.financial.cloud.dto.book;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookInitBalanceImportResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int success;
    private int failed;
    private List<RowError> errors = new ArrayList<>();

    @Data
    public static class RowError implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private int row;
        private String code;
        private String message;
    }
}
