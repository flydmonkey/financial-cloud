package com.financial.cloud.domain.expense;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 费用报销单明细：一张报销单多笔费用，每行一个费用科目。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("expense_claim_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    /** 报销单ID */
    private String claimId;

    /** 费用科目编码（借方） */
    private String expenseSubjectCode;

    /** 费用科目名称（冗余展示） */
    private String expenseSubjectName;

    /** 金额 */
    private BigDecimal amount;

    /** 费用说明 */
    private String summary;

    /** 行序 */
    private Integer sortIndex;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
