package com.financial.cloud.dto.permissions;

import java.util.ArrayList;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record PermissionBookDto(
		@NotBlank String userId,
		@NotEmpty ArrayList<String> bookIds,
		@NotBlank String roleId
) {
}
