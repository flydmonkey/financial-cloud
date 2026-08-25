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

package com.jinbooks.authn.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import com.jinbooks.authn.SignedPrincipal;
import com.jinbooks.authn.session.Session;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.util.AuthorizationHeaderUtils;
import com.jinbooks.context.WebConstants;
import com.jinbooks.context.WebContext;

/**
 * 认证信息工具类
 */
public class AuthorizationUtils {
	private static final Logger logger = LoggerFactory.getLogger(AuthorizationUtils.class);

	public class BearerType {
		public static final String CONGRESS = "congress";
		public static final String CONGRESS_TYPE = "cookie";
		public static final String PARAMETER = "congress";
		public static final String PARAMETER_TYPE = "parameter";
		public static final String AUTHORIZATION = "Authorization";
		public static final String AUTHORIZATION_TYPE = "Authorization";
	}

	public static void authenticate(HttpServletRequest request, SessionManager sessionManager) {
		String bearerType = BearerType.AUTHORIZATION_TYPE;
		String sessionId = AuthorizationHeaderUtils.resolveBearer(request);
		logger.trace("bearerType {} , sessionId {}", bearerType, sessionId);

		if (StringUtils.isBlank(sessionId)) {
			sessionId = request.getParameter(BearerType.CONGRESS);
			bearerType = BearerType.PARAMETER_TYPE;
		}

		if (StringUtils.isBlank(sessionId)) {
			Cookie authCookie = WebContext.getCookie(request, BearerType.CONGRESS);
			if (authCookie != null) {
				sessionId = authCookie.getValue();
				bearerType = BearerType.CONGRESS_TYPE;
			}
		}

		doSessionAuthenticate(request, bearerType, sessionId, sessionManager);
	}

	public static void doSessionAuthenticate(
			HttpServletRequest request,
			String bearerType,
			String sessionId,
			SessionManager sessionManager) {
		if (StringUtils.isBlank(sessionId) || sessionId.equalsIgnoreCase("undefined")) {
			clearAuthentication(request);
			return;
		}

		Session session = sessionManager.get(sessionId);
		if (session == null || session.getAuthentication() == null) {
			logger.debug("Session {} not found or expired.", sessionId);
			clearAuthentication(request);
			return;
		}

		sessionManager.refresh(sessionId);
		setAuthentication(request, session.getAuthentication());
		logger.debug("Authenticated by session {}, type {}", sessionId, bearerType);
	}

	public static Authentication getAuthentication() {
		HttpServletRequest request = WebContext.getRequest();
		return request == null ? null : getAuthentication(request);
	}

	public static Authentication getAuthentication(HttpServletRequest request) {
		return (Authentication) request.getSession().getAttribute(WebConstants.AUTHENTICATION);
	}

	public static void setAuthentication(HttpServletRequest request, Authentication authentication) {
		if (request != null) {
			request.getSession().setAttribute(WebConstants.AUTHENTICATION, authentication);
		} else {
			setAuthentication(authentication);
		}
	}

	public static void setAuthentication(Authentication authentication) {
		WebContext.setAttribute(WebConstants.AUTHENTICATION, authentication);
	}

	public static void clearAuthentication() {
		WebContext.removeAttribute(WebConstants.AUTHENTICATION);
	}

	public static void clearAuthentication(HttpServletRequest request) {
		if (request != null) {
			request.getSession().removeAttribute(WebConstants.AUTHENTICATION);
		} else {
			clearAuthentication();
		}
	}

	public static boolean isAuthenticated() {
		return getAuthentication() != null;
	}

	public static boolean isNotAuthenticated() {
		return !isAuthenticated();
	}

	public static SignedPrincipal getPrincipal() {
		Authentication authentication = getAuthentication();
		return authentication == null ? null : getPrincipal(authentication);
	}

	public static SignedPrincipal getPrincipal(Authentication authentication) {
		return authentication == null ? null : (SignedPrincipal) authentication.getPrincipal();
	}

	public static UserInfo getUserInfo(Authentication authentication) {
		SignedPrincipal principal = getPrincipal(authentication);
		return principal == null ? null : principal.getUserInfo();
	}

	public static UserInfo getUserInfo() {
		return getUserInfo(getAuthentication());
	}

	public static User getUser() {
		return (User) getAuthentication().getPrincipal();
	}
}
