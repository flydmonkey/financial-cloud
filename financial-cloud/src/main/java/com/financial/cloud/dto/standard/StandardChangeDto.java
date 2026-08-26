package com.financial.cloud.dto.standard;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

@Data
public class StandardChangeDto {

    @NotEmpty(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    String id;

    @NotEmpty(message = MessageKeys.Validation.STANDARD_ACCOUNTING_STANDARD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 21, message = MessageKeys.Validation.STANDARD_ACCOUNTING_STANDARD_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    String name;

    @NotNull(message = MessageKeys.Validation.COMMON_STATUS_NOT_NULL, groups = {AddGroup.class, EditGroup.class})
    Integer status;
}
