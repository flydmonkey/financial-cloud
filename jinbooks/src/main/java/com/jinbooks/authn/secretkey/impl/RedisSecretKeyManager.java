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
 



package com.jinbooks.authn.secretkey.impl;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jinbooks.authn.LoginCredential;
import com.jinbooks.authn.LoginSecretKey;
import com.jinbooks.authn.secretkey.SecretKeyManager;
import com.jinbooks.authn.secretkey.SecretKeyProvider;
import com.jinbooks.exception.BusinessException;

import tools.jackson.databind.json.JsonMapper;

public class RedisSecretKeyManager  implements SecretKeyManager{
	private static final Logger logger = LoggerFactory.getLogger(RedisSecretKeyManager.class);

	//旧key缓存24小时后过期
	public static final  int CACHED_DELAY_EXPIRES = 24;

	public static final  int REDIS_CACHED_DELAY_EXPIRES = CACHED_DELAY_EXPIRES * 3600;

	//实际key有效时间48小时
	public static final  int CACHED_EXPIRES = CACHED_DELAY_EXPIRES  * 2;

	public static final  int REDIS_CACHED_EXPIRES = CACHED_EXPIRES  * 2 * 3600;

	SecretKeyProvider secretKeyProvider;

	private final StringRedisTemplate redis;
	private final JsonMapper jsonMapper;

	public RedisSecretKeyManager(StringRedisTemplate redis, JsonMapper jsonMapper) {
		this.redis = redis;
		this.jsonMapper = jsonMapper;
		secretKeyProvider = new SecretKeyProvider(SecretKeyProvider.AlgorithmType.SM2,CACHED_EXPIRES);
	}

	@Override
	public  LoginSecretKey generate() {
		try {
			LoginSecretKey loginSecretKey = read(CACHED_KEY_NAME);
			if(loginSecretKey != null) {
				//旧key设置过期时间
				write(CACHED_KEY_LAST_NAME.formatted(loginSecretKey.getSecretKey()),
						loginSecretKey, REDIS_CACHED_EXPIRES);
			}
			loginSecretKey =  secretKeyProvider.generator();
			logger.info("login SecretKey {}",loginSecretKey);
			//取默认数据
			write(CACHED_KEY_NAME, loginSecretKey, REDIS_CACHED_DELAY_EXPIRES);
			write(CACHED_KEY_LAST_NAME.formatted(loginSecretKey.getSecretKey()),
					loginSecretKey, REDIS_CACHED_EXPIRES);
			return loginSecretKey;
    	 } catch (Exception e) {
    		 logger.error("Exception login SecretKey",e);
         }
		return null;
	}

	@Override
	public  LoginSecretKey getSecretKey() {
		LoginSecretKey loginSecretKey = read(CACHED_KEY_NAME);
		if(loginSecretKey == null) {
			loginSecretKey = generate();
		}
		return loginSecretKey;
	}

	@Override
	public  String decrypt(String secretKey,String cipherText ) throws Exception {
		LoginSecretKey loginSecretKey = read(CACHED_KEY_LAST_NAME.formatted(secretKey));
		if(loginSecretKey == null) {
			throw new BusinessException(12112,"SecretKey not Present");
		}
		String text = null;
		//使用私钥解密
		byte[] decrypt = secretKeyProvider.decrypt(cipherText, loginSecretKey.getPrivateKey());
		if(decrypt != null) {
			text = new String (decrypt);
		}
		return text;
	}

	@Override
	public void decrypt(LoginCredential credential) {
		if(StringUtils.isNotBlank(credential.getSecretKey())) {
    		try {
				String password = decrypt(credential.getSecretKey(), credential.getPassword());
				credential.setPassword(password);
			} catch (Exception e) {
				logger.error("decrypt Exception", e);
			}
    	}
	}

	private void write(String key, LoginSecretKey value, int validitySeconds) {
		try {
			redis.opsForValue().set(key, jsonMapper.writeValueAsString(value),
					Duration.ofSeconds(validitySeconds));
		} catch (Exception e) {
			throw new IllegalStateException("redis secret key write failed", e);
		}
	}

	private LoginSecretKey read(String key) {
		String json = redis.opsForValue().get(key);
		if (json == null) {
			return null;
		}
		try {
			return jsonMapper.readValue(json, LoginSecretKey.class);
		} catch (Exception e) {
			throw new IllegalStateException("redis secret key read failed", e);
		}
	}

}
