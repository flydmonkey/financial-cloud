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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.session.impl.InMemorySessionManager;
import com.jinbooks.authn.handler.HttpSessionListenerAdapter;
import com.jinbooks.authn.handler.SavedRequestAwareAuthenticationSuccessHandler;
import com.jinbooks.domain.security.ConfigLoginPolicy;
import com.jinbooks.service.security.ConfigLoginPolicyService;

@AutoConfiguration
public class SessionAutoConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(SessionAutoConfiguration.class);

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
		logger.debug("InMemory session timeout {}s", validitySeconds);
		return new InMemorySessionManager(validitySeconds);
	}

	@Bean
	HttpSessionListenerAdapter httpSessionListenerAdapter() {
		return new HttpSessionListenerAdapter();
	}
}
