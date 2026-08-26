package com.financial.cloud.dto.config;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class ConfigSalaryFormulaChangeDto {

    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    @NotNull(message = MessageKeys.Validation.COMMON_RULE_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String ruleName;

    String ruleDescription;

    List<ConfigSalaryItem> formulaItems;

    String formulaString;

    /**
     * 状态:1-启用;0-禁用
     */
    private Integer status;
}
