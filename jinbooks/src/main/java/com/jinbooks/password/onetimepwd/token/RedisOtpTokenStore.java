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
 





package com.jinbooks.password.onetimepwd.token;

import com.jinbooks.password.onetimepwd.OneTimePassword;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.jinbooks.constants.ConstsTimeInterval;
import com.jinbooks.entity.idm.UserInfo;

import tools.jackson.databind.json.JsonMapper;

public class RedisOtpTokenStore  extends AbstractOtpTokenStore {

    protected int validitySeconds = ConstsTimeInterval.ONE_MINUTE * 5;

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public RedisOtpTokenStore(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    public static final String PREFIX = "jb:otp:%s:%s:%s";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void store(UserInfo userInfo, String token, String receiver, String type) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        OneTimePassword otp = new OneTimePassword();
        otp.setId(formatKey(userInfo.getUsername() ,type , token));
        otp.setType(type);
        otp.setUsername(userInfo.getUsername());
        otp.setToken(token);
        otp.setReceiver(receiver);
        otp.setCreateTime(currentDateTime.format(FORMATTER));
        try {
            redis.opsForValue().set(formatKey(userInfo.getUsername(), type, token),
                    jsonMapper.writeValueAsString(otp), Duration.ofSeconds(validitySeconds));
        } catch (Exception e) {
            throw new IllegalStateException("redis OTP store failed", e);
        }
    }

    @Override
    public boolean validate(UserInfo userInfo, String token, String type, int interval) {
        String key = formatKey(userInfo.getUsername(), type, token);
        String json = redis.opsForValue().get(key);
        redis.delete(key);
        if (json == null) {
            return false;
        }
        try {
            return jsonMapper.readValue(json, OneTimePassword.class) != null;
        } catch (Exception e) {
            throw new IllegalStateException("redis OTP validation failed", e);
        }
    }

    private String formatKey(String username,String type,String token) {
    	return PREFIX.formatted(username,type,token);
    }

}
