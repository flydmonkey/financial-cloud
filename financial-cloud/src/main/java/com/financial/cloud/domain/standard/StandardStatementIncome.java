package com.financial.cloud.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serializable;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("standard_statement_income")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementIncome extends BaseEntity implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1364790594151305735L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.STANDARD_STANDARD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String standardId;


    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemCode;
    
    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    private Integer sortIndex;

    private Integer level;

    private String parentItemCode;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_CALCULATION_METHOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String symbol;
    private String subjectFlag;
    
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private List<StandardStatementRules> rules;

}
