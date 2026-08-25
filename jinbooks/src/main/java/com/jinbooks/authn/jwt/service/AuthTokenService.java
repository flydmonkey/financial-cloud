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

package com.jinbooks.authn.jwt.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.jinbooks.authn.jwt.AuthJwt;
import com.jinbooks.context.WebContext;

/**
 * 认证令牌服务：内存会话 ID 作为 token，不再签发 JWT。
 */
public class AuthTokenService {
	private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

	private final int expiresInSeconds;

	public AuthTokenService(int expiresInSeconds) {
		this.expiresInSeconds = expiresInSeconds;
	}

	public String genState() {
		return WebContext.genId();
	}

	public boolean validateState(String state) {
		return StringUtils.isNotBlank(state);
	}

	public AuthJwt genAuthJwt(Authentication authentication) {
		if (authentication == null) {
			return null;
		}
		String sessionId = ((com.jinbooks.authn.SignedPrincipal) authentication.getPrincipal()).getSessionId();
		logger.trace("issue session token {}", sessionId);
		return new AuthJwt(sessionId, authentication, expiresInSeconds, sessionId);
	}

	public int getExpiresInSeconds() {
		return expiresInSeconds;
	}
}
