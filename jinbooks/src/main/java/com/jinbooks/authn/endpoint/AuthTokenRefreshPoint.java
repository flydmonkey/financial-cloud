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

package com.jinbooks.authn.endpoint;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@Controller
@RequestMapping(value = "/auth")
public class AuthTokenRefreshPoint {
	private static final Logger logger = LoggerFactory.getLogger(AuthTokenRefreshPoint.class);

	@Autowired
	AuthTokenService authTokenService;

	@Autowired
	SessionManager sessionManager;

	@GetMapping(value = { "/token/refresh" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> refreshGet(HttpServletRequest request,
			@RequestParam(name = "refresh_token", required = false) String refreshToken) {
		return refresh(request, refreshToken);
	}

	@PostMapping(value = { "/token/refresh" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> refresh(HttpServletRequest request,
			@RequestParam(name = "refresh_token", required = false) String refreshToken) {
		logger.debug("try to refresh session");
		try {
			if (StringUtils.isBlank(refreshToken)) {
				return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
			}
			Session session = sessionManager.get(refreshToken);
			if (session == null || session.getAuthentication() == null) {
				logger.debug("refresh session not found.");
				return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
			}
			sessionManager.refresh(refreshToken);
			AuthJwt authJwt = authTokenService.genAuthJwt(session.getAuthentication());
			if (authJwt != null) {
				logger.trace("Grant refreshed session {}", refreshToken);
				return new Message<AuthJwt>(authJwt).buildResponse();
			}
		} catch (Exception e) {
			logger.error("Refresh Exception !", e);
		}
		return new ResponseEntity<>("Refresh Token Fail !", HttpStatus.UNAUTHORIZED);
	}
}
