package com.financial.cloud.dto.workspace;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BooksBoardRowVo {
    private String bookId;
    private String bookName;
    private String companyName;
    private String currentTerm;
    private boolean voucherReviewed;
    private long pendingAuditCount;
    private long pendingPostCount;
    private boolean depreciationPending;
    /** CLOSED / OPEN / BEHIND / UNKNOWN */
    private String closeStatus;
    /** book.status: 1 enable / 0 disable / 2 sealed */
    private Integer bookStatus;
    private boolean sealed;
    /** AUDIT / POST / DEPRECIATION / READY_CLOSE / READY_PACK / BEHIND / NONE */
    private String blocker;
}
