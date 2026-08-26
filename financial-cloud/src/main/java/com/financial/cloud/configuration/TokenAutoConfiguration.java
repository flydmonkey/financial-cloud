package com.financial.cloud.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.service.security.ConfigLoginPolicyService;

@Configuration
public class TokenAutoConfiguration {

	@Bean
	AuthTokenService authTokenService(ConfigLoginPolicyService configLoginPolicyService) {
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
		int expires = 8 * 3600;
		if (configLoginPolicy != null) {
			expires = configLoginPolicy.getTokenValidity() * 3600;
		}
		return new AuthTokenService(expires);
	}
}
