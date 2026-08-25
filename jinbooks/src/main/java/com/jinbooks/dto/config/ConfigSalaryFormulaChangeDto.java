package com.jinbooks.dto.config;

import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/10 10:09
 */

@Data
public class ConfigSalaryFormulaChangeDto {

    @NotNull(message = "编辑对象不能为空", groups = {EditGroup.class})
    private String id;

    @NotNull(message = "规则名称不能为空", groups = {AddGroup.class, EditGroup.class})
    String ruleName;

    String ruleDescription;

    List<ConfigSalaryItem> formulaItems;

    String formulaString;

    /**
     * 状态:1-启用;0-禁用
     */
    private Integer status;
}
