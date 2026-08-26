package com.financial.cloud.domain.voucher;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("voucher_item_cash_flow")
public class VoucherItemCashFlow extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 5584043579878024120L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String voucherItemId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cashFlowItemCode;

    private BigDecimal cashFlowBalance;

    private Integer cashFlowItemType;

    private String bookId;

    @TableField(exist = false)
    private String voucherId;

    /**
     * 金额方向
     */
    @TableField(exist = false)
    private Integer direction;

    /**
     * 科目借贷方向
     */
    @TableField(exist = false)
    private Integer subjectDirection;
}
