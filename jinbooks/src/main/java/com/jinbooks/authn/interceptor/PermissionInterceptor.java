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

package com.jinbooks.authn.interceptor;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import com.jinbooks.authn.SignedPrincipal;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.configuration.ApplicationConfig;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.auth.LoginService;

import tools.jackson.databind.json.JsonMapper;

/**
 * 登录认证判断
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(PermissionInterceptor.class);

	@Autowired
	ApplicationConfig applicationConfig;

	@Autowired
	SessionManager sessionManager;

	@Autowired
	LoginService loginService;

	private final JsonMapper jsonMapper;

	public PermissionInterceptor(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		logger.trace("Permission Interceptor .");
		AuthorizationUtils.authenticate(request, sessionManager);
		SignedPrincipal principal = AuthorizationUtils.getPrincipal();
		if (principal == null) {
			logger.trace("No Authentication for URI {}", request.getRequestURI());
			writeUnauthorized(response);
			return false;
		}
		UserInfo userInfo = principal.getUserInfo();
		if (userInfo != null) {
			List<GrantedAuthority> authorities = loginService.grantAuthority(userInfo);
			principal.setAuthenticated(true);
			UsernamePasswordAuthenticationToken authenticationToken =
					new UsernamePasswordAuthenticationToken(principal, null, authorities);
			AuthorizationUtils.setAuthentication(request, authenticationToken);
		}
		return true;
	}

	private void writeUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		Message<Void> body = new Message<>(Message.UNAUTHORIZED, "Unauthorized");
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
