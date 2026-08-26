package com.financial.cloud.dto.idm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class RoleMemberDto {
    String type;

    @NotEmpty(message = MessageKeys.Validation.USER_ROLE_REQUIRED)
    String roleId;

    @Valid
    @NotEmpty(message = MessageKeys.Validation.ORG_SELECTED_MEMBER_REQUIRED)
    List<String> memberIds;
}
