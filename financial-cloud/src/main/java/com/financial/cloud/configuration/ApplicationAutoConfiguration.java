package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.MessageSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.financial.cloud.configuration.IdStrategyConfig;
import com.financial.cloud.configuration.LegacyPasswordEncoders;
import com.financial.cloud.util.LegacySecretCodec;
import com.financial.cloud.service.security.ConfigPasswordPolicyService;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;
import com.financial.cloud.util.IdGenerator;
import com.financial.cloud.context.WebContext;
import cn.hutool.core.util.IdUtil;

@Slf4j
@Configuration
public class ApplicationAutoConfiguration {

	@Bean
	TaskExecutor historyTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(256);
		executor.setThreadNamePrefix("history-login-");
		executor.initialize();
		return executor;
	}

	@Bean
	LegacySecretCodec legacySecretCodec(
			@Value("${financial-cloud.security.legacy-secret-suffix:l0JqT7NvIzP9oRaG4kFc1QmD_bWu3x8E5yS2h6}") String keySuffix) {
		return new LegacySecretCodec(keySuffix);
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
