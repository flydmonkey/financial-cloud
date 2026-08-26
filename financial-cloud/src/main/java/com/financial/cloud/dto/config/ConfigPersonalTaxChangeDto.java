package com.financial.cloud.dto.config;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class ConfigPersonalTaxChangeDto {

    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    @NotNull(message = MessageKeys.Validation.COMMON_LEVEL_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer level;

    Integer minNum;

    Integer maxNum;

    @NotNull(message = MessageKeys.Validation.CONFIG_TAX_RATE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer taxRate;

    String bookId;

    Integer type;

    // 获取真实税率（将整数转换为小数）
    public double getRealRate() {
        return taxRate / 100.0;
    }
}
