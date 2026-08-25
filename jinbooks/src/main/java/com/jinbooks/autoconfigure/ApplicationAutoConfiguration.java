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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jinbooks.configuration.IdStrategyConfig;
import com.jinbooks.configuration.LegacyPasswordEncoders;
import com.jinbooks.util.LegacySecretCodec;
import com.jinbooks.service.security.ConfigPasswordPolicyService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;
import com.jinbooks.service.security.impl.PasswordPolicyValidatorServiceImpl;
import com.jinbooks.util.IdGenerator;
import com.jinbooks.util.SnowFlakeId;
import com.jinbooks.context.WebContext;

@AutoConfiguration
public class ApplicationAutoConfiguration {
	static final Logger logger = LoggerFactory.getLogger(ApplicationAutoConfiguration.class);

	@Bean
	LegacySecretCodec legacySecretCodec() {
		return LegacySecretCodec.getInstance();
	}

	@Bean
	DataSourceTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		PasswordEncoder passwordEncoder = LegacyPasswordEncoders.create();
		logger.info("bcrypt is default encoder");
		return passwordEncoder;
	}

	@Bean
	PasswordPolicyValidatorService passwordPolicyValidatorService(
			ConfigPasswordPolicyService configPasswordPolicyService,
			MessageSource messageSource) {
		return new PasswordPolicyValidatorServiceImpl(configPasswordPolicyService, messageSource);
	}

	@Bean
	IdGenerator idGenerator(IdStrategyConfig idStrategyConfig) {
		IdGenerator idGenerator = new IdGenerator(idStrategyConfig.getStrategy());
		SnowFlakeId snowFlakeId = new SnowFlakeId(idStrategyConfig.getDatacenterId(), idStrategyConfig.getMachineId());
		idGenerator.setSnowFlakeId(snowFlakeId);
		WebContext.setIdGenerator(idGenerator);
		return idGenerator;
	}
}
