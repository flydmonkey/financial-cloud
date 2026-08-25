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
 





package com.jinbooks.persistence.cache;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.json.JsonMapper;

public class RedisCacheService implements MemCacheService {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

	private int validitySeconds = 60 * 5;

	public static final String PREFIX = "jb:momentary:%s:%s";

	private final StringRedisTemplate redis;
	private final JsonMapper jsonMapper;

	public RedisCacheService(StringRedisTemplate redis, JsonMapper jsonMapper) {
		this.redis = redis;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public  void put(String sessionId , String name, Object value){
		put(formatKey(sessionId , name), value);
	}

    @Override
    public Object get(String sessionId , String name) {
    	return get(formatKey(sessionId , name));
    }

	@Override
	public Object remove(String sessionId, String name) {
		return remove(formatKey(sessionId , name));
	}

    private String formatKey(String sessionId , String name) {
    	return PREFIX.formatted(sessionId,name);
    }

	@Override
	public void put(String key, Object value) {
		try {
			redis.opsForValue().set(key, jsonMapper.writeValueAsString(value),
					Duration.ofSeconds(validitySeconds));
			logger.trace("key {}, validitySeconds {}, value {}", key, validitySeconds, value);
		} catch (Exception e) {
			throw new IllegalStateException("redis put failed", e);
		}
	}

	@Override
	public Object get(String key) {
		String json = redis.opsForValue().get(key);
		if (json == null) {
			return null;
		}
		try {
			Object value = jsonMapper.readValue(json, Object.class);
			logger.trace("key {}, value {}", key, value);
			return value;
		} catch (Exception e) {
			throw new IllegalStateException("redis get failed", e);
		}
	}

	@Override
	public Object remove(String key) {
		Object value = get(key);
		redis.delete(key);
		logger.trace("key {}, value {}", key, value);
		return value;
	}

}
