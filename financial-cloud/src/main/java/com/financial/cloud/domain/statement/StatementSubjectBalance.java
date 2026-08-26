package com.financial.cloud.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_subject_balance")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementSubjectBalance extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;
    private String yearPeriod;

    private String periodType;

    private Integer sortIndex;

    private String sourceId;

    private String parentId;

    private String subjectCode;

    private String subjectName;

    String direction;

    private BigDecimal balance;

    private BigDecimal openingBalanceDebit;

    private BigDecimal openingBalanceCredit;

    private BigDecimal openingYearBalanceDebit;

    private BigDecimal openingYearBalanceCredit;

    private BigDecimal currentPeriodDebit;

    private BigDecimal currentPeriodCredit;

    private BigDecimal yearToDateDebit;

    private BigDecimal yearToDateCredit;

    private BigDecimal closingBalanceDebit;

    private BigDecimal closingBalanceCredit;

    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevBalance;
    

    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevClosingBalanceDebit;

    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevClosingBalanceCredit;
    
    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevYearToDateDebit;

    @TableField(fill = FieldFill.INSERT)
    private BigDecimal prevYearToDateCredit;
    
    private String isAuxiliary;

    private String isVoucher;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
