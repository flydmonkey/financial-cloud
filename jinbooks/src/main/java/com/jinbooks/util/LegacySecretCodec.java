/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jinbooks.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

import cn.hutool.core.util.HexUtil;

/**
 * Legacy reversible secret codec for stored credentials (email/SMS/social client secrets).
 * Keeps backward compatibility with existing DESede-encrypted values in the database.
 */
public final class LegacySecretCodec implements PasswordEncoder {

	private static final int PREFIX_LENGTH = 7;
	private static final String DEFAULT_SECRET_KEY = "l0JqT7NvIzP9oRaG4kFc1QmD_bWu3x8E5yS2h6";
	private static final String DESEDE = "DESede";

	private static final LegacySecretCodec INSTANCE = new LegacySecretCodec();

	private LegacySecretCodec() {
	}

	public static LegacySecretCodec getInstance() {
		return INSTANCE;
	}

	public String decoder(CharSequence encodedPassword) {
		String salt = encodedPassword.subSequence(0, 29).toString();
		String cipherHex = encodedPassword.subSequence(29, encodedPassword.length()).toString();
		String plain = decodeHex(cipherHex, salt.substring(PREFIX_LENGTH));
		return plain.substring(salt.substring(PREFIX_LENGTH).length());
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return encode(rawPassword, encodedPassword.subSequence(0, 29).toString()).equals(encodedPassword);
	}

	@Override
	public String encode(CharSequence plain) {
		return encode(plain, BCrypt.gensalt("$2a", 10));
	}

	private String encode(CharSequence plain, String salt) {
		String password = salt.substring(PREFIX_LENGTH) + plain;
		return salt + encodeHex(password, salt.substring(PREFIX_LENGTH));
	}

	private static String encodeHex(String plain, String secretKey) {
		byte[] cipher = encrypt(plain.getBytes(StandardCharsets.UTF_8), desedeKey(secretKey));
		return HexUtil.encodeHexStr(cipher);
	}

	private static String decodeHex(String cipherHex, String secretKey) {
		if (StringUtils.isBlank(cipherHex)) {
			return "";
		}
		byte[] plain = decrypt(HexUtil.decodeHex(cipherHex), desedeKey(secretKey));
		return new String(plain, StandardCharsets.UTF_8);
	}

	private static String desedeKey(String secretKey) {
		return (secretKey + DEFAULT_SECRET_KEY).substring(0, 24);
	}

	private static byte[] encrypt(byte[] plain, String key) {
		try {
			SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), DESEDE);
			Cipher cipher = Cipher.getInstance(DESEDE);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return cipher.doFinal(plain);
		} catch (Exception e) {
			throw new IllegalStateException("encrypt failed", e);
		}
	}

	private static byte[] decrypt(byte[] cipherBytes, String key) {
		try {
			SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), DESEDE);
			Cipher cipher = Cipher.getInstance(DESEDE);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return cipher.doFinal(cipherBytes);
		} catch (Exception e) {
			throw new IllegalStateException("decrypt failed", e);
		}
	}
}
