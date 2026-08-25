package com.jinbooks.dto.idm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleMemberDto {
    String type;

    @NotEmpty(message = "角色不能为空")
    String roleId;

    @Valid
    @NotEmpty(message = "所选成员不能为空")
    List<String> memberIds;
}
