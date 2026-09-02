package com.financial.cloud.domain.fixedasset;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fixed_asset")
public class FixedAsset extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String bookId;
    private String code;
    private String name;
    private String categoryId;
    private String deptId;
    private Date startUseDate;
    private String entryPeriod;
    private Integer quantity;
    private String spec;
    private String location;
    private String userId;
    private String status;
    private String disposedPeriod;
    private String suspendedPeriod;
    private String depreciationMethod;
    private Integer usefulLifeMonths;
    private BigDecimal expectedTotalWork;
    private BigDecimal residualRate;
    private BigDecimal originalValue;
    private BigDecimal taxAmount;
    private BigDecimal impairment;
    private Integer depreciatedPeriods;
    private BigDecimal openingAccumDepr;
    private BigDecimal accumDepr;
    private BigDecimal yearDepr;
    private String fixedAssetSubjectId;
    private String purchaseCounterpartSubjectId;
    private String taxSubjectId;
    private String accumDeprSubjectId;
    private String expenseSubjectId;
    private String disposalSubjectId;
    private String impairmentSubjectId;
    private String impairmentCounterpartSubjectId;
    private String remark;
    private String disposeVoucherId;
    private String purchaseVoucherId;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
