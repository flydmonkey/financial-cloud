package com.jinbooks.dto.idm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleMemberUserGroupsDto {

    @NotEmpty(message = "用户名称不能为空")
    String username;

    @Valid
    @NotEmpty(message = "所选角色集合不能为空")
    List<String> groupIds;
}
