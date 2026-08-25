package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.session.impl.InMemorySessionManager;
import com.jinbooks.authn.handler.HttpSessionListenerAdapter;
import com.jinbooks.authn.handler.SavedRequestAwareAuthenticationSuccessHandler;
import com.jinbooks.domain.security.ConfigLoginPolicy;
import com.jinbooks.service.security.ConfigLoginPolicyService;

@Slf4j
@Configuration
public class SessionAutoConfiguration {

	@Bean(name = "savedRequestSuccessHandler")
	SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler() {
		return new SavedRequestAwareAuthenticationSuccessHandler();
	}

	@Bean(name = "sessionManager")
	SessionManager sessionManager(
			ConfigLoginPolicyService configLoginPolicyService,
			@Value("${jinbooks.session.max-size:10000}") int maxSize,
			@Value("${jinbooks.session.default-validity-hours:8}") int defaultValidityHours) {
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
		int validitySeconds = defaultValidityHours * 3600;
		if (configLoginPolicy != null && configLoginPolicy.getSessionValidity() > 0) {
			validitySeconds = configLoginPolicy.getSessionValidity() * 3600;
		}
		log.info("Using in-memory session store for single-node deployment (timeout {}s)", validitySeconds);
		return new InMemorySessionManager(validitySeconds, maxSize);
	}

	@Bean
	HttpSessionListenerAdapter httpSessionListenerAdapter() {
		return new HttpSessionListenerAdapter();
	}
}
