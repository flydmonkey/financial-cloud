package com.financial.cloud.authn.endpoint;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.financial.cloud.authn.jwt.AuthJwt;
import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.common.Message;

/**
 * 前端认证令牌刷新：延长内存会话有效期。
 */
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping(value = "/api/auth")
public class AuthTokenRefreshPoint {

	private static final String REFRESH_FAILED_MESSAGE = "Refresh Token Fail !";

	private final AuthTokenService authTokenService;

	private final SessionManager sessionManager;

	@GetMapping(value = { "/token/refresh" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> refreshGet(HttpServletRequest request,
			@RequestParam(name = "refresh_token", required = false) String refreshToken) {
		return refresh(request, refreshToken);
	}

	@PostMapping(value = { "/token/refresh" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> refresh(HttpServletRequest request,
			@RequestParam(name = "refresh_token", required = false) String refreshToken) {
		log.debug("try to refresh session");
		try {
			if (StringUtils.isBlank(refreshToken)) {
				return unauthorized(REFRESH_FAILED_MESSAGE);
			}
			Session session = sessionManager.get(refreshToken);
			if (session == null || session.getAuthentication() == null) {
				log.debug("refresh session not found.");
				return unauthorized(REFRESH_FAILED_MESSAGE);
			}
			sessionManager.refresh(refreshToken);
			AuthJwt authJwt = authTokenService.genAuthJwt(session.getAuthentication());
			if (authJwt != null) {
				log.trace("Grant refreshed session {}", refreshToken);
				return new Message<>(authJwt).buildResponse();
			}
		} catch (Exception e) {
			log.error("Refresh Exception !", e);
		}
		return unauthorized(REFRESH_FAILED_MESSAGE);
	}

	private ResponseEntity<Message<Void>> unauthorized(String message) {
		return new Message<Void>(Message.UNAUTHORIZED, message).buildUnauthorizedResponse();
	}
}
