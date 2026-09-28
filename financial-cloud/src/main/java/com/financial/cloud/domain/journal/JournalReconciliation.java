package com.financial.cloud.domain.journal;

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
 * 银行对账单余额：按账户 + 期间登记银行对账单期末余额，用于余额调节表。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("journal_reconciliation")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalReconciliation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String bookId;

    private String accId;

    /** 对账期间 yyyy-MM */
    private String yearPeriod;

    /** 银行对账单期末余额 */
    private BigDecimal statementBalance;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
