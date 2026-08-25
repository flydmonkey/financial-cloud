package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
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
	SessionManager sessionManager(ConfigLoginPolicyService configLoginPolicyService) {
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
		int validitySeconds = 8 * 3600;
		if (configLoginPolicy != null) {
			validitySeconds = configLoginPolicy.getSessionValidity() * 3600;
		}
		log.debug("InMemory session timeout {}s", validitySeconds);
		return new InMemorySessionManager(validitySeconds);
	}

	@Bean
	HttpSessionListenerAdapter httpSessionListenerAdapter() {
		return new HttpSessionListenerAdapter();
	}
}
