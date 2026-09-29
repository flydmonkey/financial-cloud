package com.financial.cloud.dto.expense;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 费用报销单保存入参：单头 + 明细行（至少一行）。
 */
@Data
public class ExpenseClaimSaveDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 更新时必传 */
    private String id;

    private String claimant;

    /** yyyy-MM-dd */
    private String claimDate;

    /** 付款科目编码（贷方：库存现金/银行存款） */
    private String fundSubjectCode;

    /** 报销事由（单头） */
    private String summary;

    /** 明细行：每行一个费用科目与金额，至少一行 */
    private List<ExpenseClaimItemDto> items;
}
