package com.financial.cloud.domain.expense;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
 * 费用报销单：暂存 → 已提交 → 已审核/已拒绝；已审核可一键生成报销凭证（暂存态）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("expense_claim")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaim extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：暂存 */
    public static final String STATUS_DRAFT = "draft";
    /** 状态：已提交 */
    public static final String STATUS_SUBMITTED = "submitted";
    /** 状态：已审核 */
    public static final String STATUS_APPROVED = "approved";
    /** 状态：已拒绝 */
    public static final String STATUS_REJECTED = "rejected";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    /** 报销单号 BXyyyyMM-序号 */
    private String claimNo;

    /** 报销人 */
    private String claimant;

    /** 报销日期 */
    private LocalDate claimDate;

    /** 费用科目编码（借方） */
    private String expenseSubjectCode;

    /** 费用科目名称（冗余展示） */
    private String expenseSubjectName;

    /** 付款科目编码（贷方：库存现金/银行存款） */
    private String fundSubjectCode;

    /** 付款科目名称（冗余展示） */
    private String fundSubjectName;

    /** 报销金额 */
    private BigDecimal amount;

    /** 报销事由 */
    private String summary;

    /** draft/submitted/approved/rejected */
    private String claimStatus;

    /** 生成的凭证ID（审核后一键生成） */
    private String voucherId;

    private String auditBy;

    private LocalDateTime auditTime;

    private String rejectReason;

    /** 明细行（非持久化，detail 接口填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<com.financial.cloud.domain.expense.ExpenseClaimItem> items;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
