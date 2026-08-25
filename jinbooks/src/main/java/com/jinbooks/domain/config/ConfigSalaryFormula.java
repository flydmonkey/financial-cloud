package com.jinbooks.domain.config;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 17:46
 */

@EqualsAndHashCode(callSuper = true)
@TableName("jbx_config_salary_formula")
@Data
public class ConfigSalaryFormula extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -39619691289916070L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    String ruleName;

    String ruleDescription;

    String formula;

    String formulaText;

    /**
     * 状态:1-启用;0-禁用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
