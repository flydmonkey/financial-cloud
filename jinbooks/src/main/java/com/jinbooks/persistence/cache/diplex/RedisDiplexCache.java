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
 





package com.jinbooks.persistence.cache.diplex;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.json.JsonMapper;

public class RedisDiplexCache implements DiplexCache{
    private static final Logger logger = LoggerFactory.getLogger(RedisDiplexCache.class);

    protected int validitySeconds 	= 60 * 30; //default 30 minutes.

	private final StringRedisTemplate redis;
	private final JsonMapper jsonMapper;

	public static final String PREFIX = "jb:diplex:";

	public String getKey(String key) {
		return PREFIX + key;
	}

	/**
	 * @param connectionFactory
	 */
	public RedisDiplexCache(StringRedisTemplate redis, JsonMapper jsonMapper, int validitySeconds) {
		this.redis = redis;
		this.jsonMapper = jsonMapper;
		this.validitySeconds = validitySeconds;
	}


	@Override
	public void putObject(String key, Object value) {
		try {
			redis.opsForValue().set(getKey(key), jsonMapper.writeValueAsString(value),
					Duration.ofSeconds(validitySeconds));
		} catch (Exception e) {
			throw new IllegalStateException("redis diplex write failed", e);
		}
	}

    @Override
    public Object getObject(String key) {
    	if(key == null) {
    		logger.debug("key can't been null .");
    		return null;
    	}
		String json = redis.opsForValue().get(getKey(key));
		if (json == null) {
			return null;
		}
		try {
			return jsonMapper.readValue(json, Object.class);
		} catch (Exception e) {
			throw new IllegalStateException("redis diplex read failed", e);
		}
    }

	@Override
	public Object removeObject(String key) {
		Object value = getObject(key);
		redis.delete(getKey(key));
		return value;
	}

	@Override
	public void put(String key, String value) {
		redis.opsForValue().set(getKey(key), value, Duration.ofSeconds(validitySeconds));
	}

	@Override
	public String get(String key) {
		if(key == null) {
    		logger.debug("key can't been null .");
    		return null;
    	}
		return redis.opsForValue().get(getKey(key));
	}

	@Override
	public String remove(String key) {
		String value = get(key);
		redis.delete(getKey(key));
		return value;
	}

	public void setValiditySeconds(int validitySeconds) {
		this.validitySeconds = validitySeconds;
	}


}
