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
 





package com.jinbooks.autoconfigure;

import com.jinbooks.password.onetimepwd.token.RedisOtpTokenStore;
import com.jinbooks.password.sms.SmsOtpAuthnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.jinbooks.configuration.ApplicationConfig;
import com.jinbooks.persistence.service.ConfigEmailSendersService;
import com.jinbooks.persistence.service.ConfigSmsProviderService;

import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
public class SmsAutoConfiguration {
    private static final  Logger logger = LoggerFactory.getLogger(SmsAutoConfiguration.class);

    @Bean(name = "smsOtpAuthnService")
    SmsOtpAuthnService smsOtpAuthnService(
    		ApplicationConfig applicationConfig,
            ConfigSmsProviderService configSmsProviderService,
            ConfigEmailSendersService configEmailSendersService,
            ObjectProvider<StringRedisTemplate> redisProvider,
			JsonMapper jsonMapper) {
        SmsOtpAuthnService smsOtpAuthnService =
        		new SmsOtpAuthnService(
                        configSmsProviderService,
                        configEmailSendersService);

        if (applicationConfig.isCachedRedis()) {
            RedisOtpTokenStore redisOptTokenStore =
					new RedisOtpTokenStore(requireRedis(redisProvider), jsonMapper);
            smsOtpAuthnService.setRedisOptTokenStore(redisOptTokenStore);
            logger.debug("SmsOtpAuthnService Redis inited.");
        }


        return smsOtpAuthnService;
    }

	private StringRedisTemplate requireRedis(ObjectProvider<StringRedisTemplate> redisProvider) {
		StringRedisTemplate redis = redisProvider.getIfAvailable();
		if (redis == null) {
			throw new IllegalStateException("Redis caching selected but StringRedisTemplate is unavailable");
		}
		return redis;
	}
}
