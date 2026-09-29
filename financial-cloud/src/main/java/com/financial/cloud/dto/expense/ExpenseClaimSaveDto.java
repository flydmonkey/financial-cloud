package com.financial.cloud.dto.expense;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 费用报销单保存入参。
 */
@Data
public class ExpenseClaimSaveDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 更新时必传 */
    private String id;

    private String claimant;

    /** yyyy-MM-dd */
    private String claimDate;

    private String expenseSubjectCode;

    private String fundSubjectCode;

    private BigDecimal amount;

    private String summary;
}
