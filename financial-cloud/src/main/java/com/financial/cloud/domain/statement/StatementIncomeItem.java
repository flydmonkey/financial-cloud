package com.financial.cloud.domain.statement;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_income_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementIncomeItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.BOOK_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    private String incomeId;


    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemCode;
    
    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_CALCULATION_METHOD_SIMPLE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String symbol;
    
    private Integer level;

    private Integer sortIndex;
    
    private BigDecimal currentBalance;

    private BigDecimal cumulativeBalance;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private List<StatementRules> rules;

}
