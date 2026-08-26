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

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("standard_statement_rules")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardStatementRules extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank(message = MessageKeys.Validation.STANDARD_STANDARD_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String standardId;
    
    @NotBlank(message = MessageKeys.Validation.STATEMENT_REPORT_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String type;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_REPORT_CATEGORY_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String itemCode;

    @NotBlank(message = MessageKeys.Validation.BOOK_SUBJECT_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String subjectCode;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_DATA_RULE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String rule;

    @NotBlank(message = MessageKeys.Validation.STATEMENT_CALCULATION_METHOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String symbol;

}
