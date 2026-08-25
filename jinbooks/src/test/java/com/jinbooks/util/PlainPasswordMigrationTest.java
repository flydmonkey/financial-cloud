package com.jinbooks.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jinbooks.configuration.LegacyPasswordEncoders;

class PlainPasswordMigrationTest {

	private final PasswordEncoder passwordEncoder = LegacyPasswordEncoders.create();

	@Test
	void detectsPlainEncodedPassword() {
		assertThat(PlainPasswordMigration.isPlainEncoded("{plain}maxkey")).isTrue();
		assertThat(PlainPasswordMigration.isPlainEncoded("{bcrypt}$2a$10$abc")).isFalse();
	}

	@Test
	void migratesPlainPasswordToBcrypt() {
		String migrated = PlainPasswordMigration.migrate("{plain}maxkey", passwordEncoder);

		assertThat(migrated).startsWith("{bcrypt}$2a$");
		assertThat(passwordEncoder.matches("maxkey", migrated)).isTrue();
	}

	@Test
	void delegatingEncoderNoLongerSupportsPlainPrefix() {
		assertThatThrownBy(() -> passwordEncoder.matches("maxkey", "{plain}maxkey"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("plain");
	}

	@Test
	void newPasswordsUseBcrypt() {
		String encoded = passwordEncoder.encode("JinBooks@888");
		assertThat(encoded).startsWith("{bcrypt}$2a$");
		assertThat(passwordEncoder.matches("JinBooks@888", encoded)).isTrue();
	}
}
