package com.financial.cloud.domain.config;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@TableName("config_salary_formula")
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
