package com.financial.cloud.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_cash_flow")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementCashFlow extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField(exist = false)
    @NotBlank(message = MessageKeys.Validation.STATEMENT_PERIOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String yearPeriod;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;


    private String periodType;

    private Integer sortIndex;

    private String itemName;

    private String itemCode;

    private BigDecimal currentAmount;

    private BigDecimal monthlyAmount;

    private String bookId;


    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private Integer isTitle;

    @TableField(exist = false)
    private Integer isMain;

    @TableField(exist = false)
    private Integer isAdditional;

    @TableField(exist = false)
    private Integer isResult;
}
