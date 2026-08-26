package com.financial.cloud.dto.idm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class RoleMemberUserGroupsDto {

    @NotEmpty(message = MessageKeys.Validation.USER_DISPLAY_NAME_REQUIRED)
    String username;

    @Valid
    @NotEmpty(message = MessageKeys.Validation.USER_SELECTED_ROLES_REQUIRED)
    List<String> groupIds;
}
