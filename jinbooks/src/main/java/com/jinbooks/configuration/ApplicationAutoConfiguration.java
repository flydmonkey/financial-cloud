package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jinbooks.configuration.IdStrategyConfig;
import com.jinbooks.configuration.LegacyPasswordEncoders;
import com.jinbooks.util.LegacySecretCodec;
import com.jinbooks.service.security.ConfigPasswordPolicyService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;
import com.jinbooks.util.IdGenerator;
import com.jinbooks.context.WebContext;
import cn.hutool.core.util.IdUtil;

@Slf4j
@Configuration
public class ApplicationAutoConfiguration {

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
		log.info("bcrypt is default encoder");
		return passwordEncoder;
	}

	@Bean
	PasswordPolicyValidatorService passwordPolicyValidatorService(
			ConfigPasswordPolicyService configPasswordPolicyService,
			MessageSource messageSource) {
		return new PasswordPolicyValidatorService(configPasswordPolicyService, messageSource);
	}

	@Bean
	SmartInitializingSingleton webContextInitializer(ApplicationContext applicationContext) {
		return () -> WebContext.init(applicationContext);
	}

	@Bean
	IdGenerator idGenerator(IdStrategyConfig idStrategyConfig) {
		IdGenerator idGenerator = new IdGenerator(idStrategyConfig.getStrategy());
		idGenerator.setSnowflake(IdUtil.getSnowflake(
				idStrategyConfig.getMachineId(),
				idStrategyConfig.getDatacenterId()));
		WebContext.setIdGenerator(idGenerator);
		return idGenerator;
	}
}
