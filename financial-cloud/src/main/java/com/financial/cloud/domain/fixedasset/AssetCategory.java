package com.financial.cloud.domain.fixedasset;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_category")
public class AssetCategory extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String bookId;
    private String code;
    private String name;
    private String depreciationMethod;
    private Integer usefulLifeYears;
    private Integer usefulLifeMonths;
    private BigDecimal residualRate;
    private String fixedAssetSubjectId;
    private String accumDeprSubjectId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
