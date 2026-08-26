package com.financial.cloud.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("standard_statement_balance_sheet")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementBalanceSheet extends BaseEntity implements Serializable {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 2355729437193604913L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.STANDARD_STANDARD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String standardId;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_ITEM_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String assetOrLiability;

    private Integer sortIndex;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemCode;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemName;


    private Integer level;

    private String parentItemCode;

    private String symbol;

    private String rule;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private List<StandardStatementRules> rules;

}
