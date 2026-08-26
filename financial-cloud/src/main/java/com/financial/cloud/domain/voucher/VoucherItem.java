package com.financial.cloud.domain.voucher;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("voucher_item")
public class VoucherItem extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属账套
     */
    private String bookId;
    
    /**
     * 凭证ID
     */
    private String voucherId;

    /**
     * 业务日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date voucherDate;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 会计科目ID
     */
    private String subjectId;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 科目编号
     */
    private String subjectCode;

    /**
     * 借方金额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

    /**
     * 数量
     */
    private Integer num;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 期初累计借方
     */
    private BigDecimal cumulativeDebit;

    /**
     * 期初累计贷方
     */
    private BigDecimal cumulativeCredit;

    /**
     * 科目余额
     */
    private BigDecimal subjectBalance;

    /**
     * 结转损益
     */
    private Integer carryForward;

    /**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
