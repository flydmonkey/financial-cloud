package com.financial.cloud.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页待办：当前账期下需要会计人员处理的事项汇总。
 * 对应 docs/product/20-gap-analysis.md §9.2。
 */
@Data
@Builder
public class DashboardTodoVo {

    /** 当前开放账期（yyyy-MM） */
    private String currentTerm;

    /** 凭证是否需要审核（账套配置） */
    private boolean voucherReviewed;

    /** 待审核凭证数（仅 voucherReviewed=true 时有意义） */
    private long pendingAuditCount;

    /** 待过账凭证数（已审核/无需审核但未过账） */
    private long pendingPostCount;

    /** 本期是否需要计提折旧且尚未计提 */
    private boolean depreciationPending;

    /** 逾期应收总额（31 天以上账龄桶，与往来月末汇总口径一致） */
    private BigDecimal overdueReceivable;

    /** 逾期应付总额（31 天以上账龄桶） */
    private BigDecimal overduePayable;
}
