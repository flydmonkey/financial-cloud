package com.financial.cloud.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(
		@NotBlank @Size(max = 32) String username,
		@NotBlank String password,
		@NotBlank @Size(max = 32) String displayName
) {
}
