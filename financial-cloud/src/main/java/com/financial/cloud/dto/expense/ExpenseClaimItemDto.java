package com.financial.cloud.dto.expense;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 费用报销单明细行入参。
 */
@Data
public class ExpenseClaimItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String expenseSubjectCode;

    private BigDecimal amount;

    private String summary;
}
