package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.session.impl.InMemorySessionManager;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.service.security.ConfigLoginPolicyService;

@Slf4j
@Configuration
public class SessionAutoConfiguration {

	@Bean(name = "sessionManager")
	SessionManager sessionManager(
			ConfigLoginPolicyService configLoginPolicyService,
			@Value("${financial-cloud.session.max-size:10000}") int maxSize,
			@Value("${financial-cloud.session.default-validity-hours:8}") int defaultValidityHours) {
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
		int validitySeconds = defaultValidityHours * 3600;
		if (configLoginPolicy != null && configLoginPolicy.getSessionValidity() > 0) {
			validitySeconds = configLoginPolicy.getSessionValidity() * 3600;
		}
		log.info("Using in-memory session store for single-node deployment (timeout {}s)", validitySeconds);
		return new InMemorySessionManager(validitySeconds, maxSize);
	}
}
