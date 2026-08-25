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
 





package com.jinbooks.authn.congress;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jinbooks.authn.jwt.AuthJwt;

import tools.jackson.databind.json.JsonMapper;

/**
 * congress Redis缓存服务
 *
 * @author Crystal.Sea
 *
 */
public class RedisCongressService implements CongressService {
    private static final Logger logger = LoggerFactory.getLogger(RedisCongressService.class);

    /**
     * 默认有效时间3分钟
     */
	protected int validitySeconds = 60 * 3; //default 3 minutes.

	private final StringRedisTemplate redis;
	private final JsonMapper jsonMapper;

	public static final String PREFIX = "jb:congress:%s";

	/**
	 * @param connectionFactory
	 */
	public RedisCongressService(StringRedisTemplate redis, JsonMapper jsonMapper) {
		this.redis = redis;
		this.jsonMapper = jsonMapper;
	}

	/**
	 * 存储
	 */
	@Override
	public void store(String congress, AuthJwt authJwt) {
		write(formatKey(congress), authJwt);
		logger.debug("store congress {} , {}", congress, authJwt);
	}

	/**
	 * 删除
	 */
	@Override
	public AuthJwt remove(String congress) {
		AuthJwt authJwt = get(congress);
		redis.delete(formatKey(congress));
		logger.debug("remove {}", congress);
		return authJwt;
	}

	/**
	 * 读取
	 */
    @Override
    public AuthJwt get(String congress) {
		return read(formatKey(congress));
    }

    /**
     * 读取
     */
	@Override
	public AuthJwt consume(String congress) {
		AuthJwt authJwt = get(congress);
		redis.delete(formatKey(congress));
		logger.debug("consume {}", congress);
		return authJwt;
	}

	public String formatKey(String congress) {
		return PREFIX.formatted(congress) ;
	}

	private void write(String key, AuthJwt value) {
		try {
			redis.opsForValue().set(key, jsonMapper.writeValueAsString(value),
					Duration.ofSeconds(validitySeconds));
		} catch (Exception e) {
			throw new IllegalStateException("redis congress write failed", e);
		}
	}

	private AuthJwt read(String key) {
		String json = redis.opsForValue().get(key);
		if (json == null) {
			return null;
		}
		try {
			return jsonMapper.readValue(json, AuthJwt.class);
		} catch (Exception e) {
			throw new IllegalStateException("redis congress read failed", e);
		}
	}
}
