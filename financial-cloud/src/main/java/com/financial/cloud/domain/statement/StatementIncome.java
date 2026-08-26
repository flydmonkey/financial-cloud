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
import java.util.List;
import java.util.Map;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("statement_income")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementIncome extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.BOOK_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_PERIOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String yearPeriod;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_REPORT_PERIOD_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String periodType;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private List<StatementIncomeItem> items;
    
    @TableField(exist = false)
    private Map<String,StatementIncomeItem> itemMap;

}
