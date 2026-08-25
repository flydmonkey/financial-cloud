package com.jinbooks.authn.endpoint;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jinbooks.authn.jwt.AuthJwt;
import com.jinbooks.authn.jwt.service.AuthTokenService;
import com.jinbooks.authn.session.Session;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.common.Message;

/**
 * 前端认证令牌刷新：延长内存会话有效期。
 */
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping(value = "/api/auth")
public class AuthTokenRefreshPoint {

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
				return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
			}
			Session session = sessionManager.get(refreshToken);
			if (session == null || session.getAuthentication() == null) {
				log.debug("refresh session not found.");
				return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
			}
			sessionManager.refresh(refreshToken);
			AuthJwt authJwt = authTokenService.genAuthJwt(session.getAuthentication());
			if (authJwt != null) {
				log.trace("Grant refreshed session {}", refreshToken);
				return new Message<AuthJwt>(authJwt).buildResponse();
			}
		} catch (Exception e) {
			log.error("Refresh Exception !", e);
		}
		return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
	}
}
