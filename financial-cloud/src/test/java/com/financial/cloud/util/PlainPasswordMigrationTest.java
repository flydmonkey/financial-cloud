package com.financial.cloud.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.financial.cloud.configuration.LegacyPasswordEncoders;

class PlainPasswordMigrationTest {

	private final PasswordEncoder passwordEncoder = LegacyPasswordEncoders.create();

	@Test
	void detectsPlainEncodedPassword() {
		assertThat(PlainPasswordMigration.isPlainEncoded("{plain}changeme")).isTrue();
		assertThat(PlainPasswordMigration.isPlainEncoded("{bcrypt}$2a$10$abc")).isFalse();
	}

	@Test
	void migratesPlainPasswordToBcrypt() {
		String migrated = PlainPasswordMigration.migrate("{plain}changeme", passwordEncoder);

		assertThat(migrated).startsWith("{bcrypt}$2a$");
		assertThat(passwordEncoder.matches("changeme", migrated)).isTrue();
	}

	@Test
	void delegatingEncoderNoLongerSupportsPlainPrefix() {
		assertThatThrownBy(() -> passwordEncoder.matches("changeme", "{plain}changeme"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("plain");
	}

	@Test
	void newPasswordsUseBcrypt() {
		String encoded = passwordEncoder.encode("FinancialCloud@888");
		assertThat(encoded).startsWith("{bcrypt}$2a$");
		assertThat(passwordEncoder.matches("FinancialCloud@888", encoded)).isTrue();
	}
}
