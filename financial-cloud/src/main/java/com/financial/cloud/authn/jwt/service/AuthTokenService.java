package com.financial.cloud.authn.jwt.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;

import com.financial.cloud.authn.jwt.AuthJwt;
import com.financial.cloud.context.WebContext;

/**
 * 认证令牌服务：内存会话 ID 作为 token，不再签发 JWT。
 */
@Slf4j
public class AuthTokenService {

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
		String sessionId = ((com.financial.cloud.authn.SignedPrincipal) authentication.getPrincipal()).getSessionId();
		log.trace("issue session token {}", sessionId);
		return new AuthJwt(sessionId, authentication, expiresInSeconds, sessionId);
	}

	public int getExpiresInSeconds() {
		return expiresInSeconds;
	}
}
