package com.financial.cloud.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import cn.hutool.crypto.SmUtil;

/**
 * Password encoders backed by Hutool (SM3) and JDK (legacy salted MD5).
 */
public final class LegacyPasswordEncoders {

	private LegacyPasswordEncoders() {
	}

	public static PasswordEncoder sm3() {
		return new PasswordEncoder() {
			@Override
			public String encode(CharSequence rawPassword) {
				return SmUtil.sm3(rawPassword.toString());
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return encodedPassword.equals(encode(rawPassword));
			}
		};
	}

	public static PasswordEncoder saltedMd5() {
		return new SaltedMd5PasswordEncoder();
	}

	public static PasswordEncoder create() {
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		String idForEncode = "bcrypt";
		encoders.put("bcrypt", new BCryptPasswordEncoder());
		encoders.put("sm3", sm3());
		encoders.put("md5", saltedMd5());
		return new DelegatingPasswordEncoder(idForEncode, encoders);
	}

	private static final class SaltedMd5PasswordEncoder implements PasswordEncoder {
		private static final String PREFIX = "{";
		private static final String SUFFIX = "}";
		private final SecureRandom random = new SecureRandom();

		@Override
		public String encode(CharSequence rawPassword) {
			String salt = PREFIX + randomSalt() + SUFFIX;
			return salt + digest(salt, rawPassword);
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			String salt = extractSalt(encodedPassword);
			return constantTimeEquals(encodedPassword, salt + digest(salt, rawPassword));
		}

		private String digest(String salt, CharSequence rawPassword) {
			byte[] hash = md5((rawPassword + salt).getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		}

		private static byte[] md5(byte[] input) {
			try {
				return MessageDigest.getInstance("MD5").digest(input);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("MD5 not available", e);
			}
		}

		private String randomSalt() {
			byte[] bytes = new byte[12];
			random.nextBytes(bytes);
			return Base64.getEncoder().encodeToString(bytes);
		}

		private static String extractSalt(String encodedPassword) {
			if (!encodedPassword.startsWith(PREFIX)) {
				return "";
			}
			int end = encodedPassword.indexOf(SUFFIX);
			return end < 0 ? "" : encodedPassword.substring(0, end + 1);
		}

		private static String bytesToHex(byte[] bytes) {
			StringBuilder sb = new StringBuilder(bytes.length * 2);
			for (byte b : bytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}

		private static boolean constantTimeEquals(String expected, String actual) {
			return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
					actual.getBytes(StandardCharsets.UTF_8));
		}
	}
}
