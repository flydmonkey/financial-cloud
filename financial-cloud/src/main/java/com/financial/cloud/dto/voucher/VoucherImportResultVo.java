package com.financial.cloud.dto.voucher;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class VoucherImportResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int success;
    private int failed;
    private List<RowError> errors = new ArrayList<>();
    private boolean needsConflictDecision;
    private int skipped;
    private List<ConflictItem> conflicts = new ArrayList<>();

    @Data
    public static class RowError implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private int row;
        private String code;
        private String message;
    }

    @Data
    public static class ConflictItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private int row;
        private String wordHead;
        private Integer wordNum;
        private String wordLabel;
        private String existingStatus;
        private String existingId;
        private boolean posted;
    }
}
