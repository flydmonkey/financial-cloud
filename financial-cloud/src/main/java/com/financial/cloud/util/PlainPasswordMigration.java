package com.financial.cloud.util;

import org.springframework.security.crypto.password.PasswordEncoder;

public final class PlainPasswordMigration {

	private static final String PLAIN_PREFIX = "{plain}";

	private PlainPasswordMigration() {
	}

	public static boolean isPlainEncoded(String encodedPassword) {
		return encodedPassword != null && encodedPassword.startsWith(PLAIN_PREFIX);
	}

	public static String migrate(String encodedPassword, PasswordEncoder passwordEncoder) {
		if (!isPlainEncoded(encodedPassword)) {
			return encodedPassword;
		}
		String rawPassword = encodedPassword.substring(PLAIN_PREFIX.length());
		return passwordEncoder.encode(rawPassword);
	}
}
