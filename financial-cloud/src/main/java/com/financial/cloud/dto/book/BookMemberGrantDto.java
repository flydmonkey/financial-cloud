package com.financial.cloud.dto.book;

import jakarta.validation.constraints.NotBlank;

public record BookMemberGrantDto(
		@NotBlank String bookId,
		@NotBlank String userId,
		@NotBlank String roleId
) {
}
