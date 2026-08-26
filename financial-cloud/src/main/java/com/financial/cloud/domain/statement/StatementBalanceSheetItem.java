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
@TableName("statement_balance_sheet_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceSheetItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.BOOK_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_MAIN_REPORT_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String balanceSheetId;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_ITEM_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String assetOrLiability;

    private String itemCode;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_FINANCIAL_ITEM_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    private Integer level;

    private Integer sortIndex;

    private String parentItemCode;

    private String symbol;

    private String rule;

    private BigDecimal currentBalance;

    private BigDecimal initialBalance;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private List<StatementRules> rules;
}
