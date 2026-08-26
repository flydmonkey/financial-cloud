package com.financial.cloud.authn.support;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.util.AuthorizationHeaderUtils;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

@Slf4j
public class AuthorizationUtils {

	public static final class BearerType {
		public static final String SESSION_COOKIE = "jb_session";
		public static final String LEGACY_SESSION_COOKIE = "congress";
		public static final String SESSION_PARAM = "jb_session";
		public static final String LEGACY_SESSION_PARAM = "congress";
		public static final String AUTHORIZATION = "Authorization";
		public static final String AUTHORIZATION_TYPE = "Authorization";
		public static final String COOKIE_TYPE = "cookie";
		public static final String PARAMETER_TYPE = "parameter";

		private BearerType() {
		}
	}

	public static void authenticate(HttpServletRequest request, SessionManager sessionManager) {
		String bearerType = BearerType.AUTHORIZATION_TYPE;
		String sessionId = AuthorizationHeaderUtils.resolveBearer(request);
		log.trace("bearerType {} , sessionId {}", bearerType, sessionId);

		if (StringUtils.isBlank(sessionId)) {
			sessionId = resolveSessionParameter(request);
			if (StringUtils.isNotBlank(sessionId)) {
				bearerType = BearerType.PARAMETER_TYPE;
			}
		}

		if (StringUtils.isBlank(sessionId)) {
			sessionId = resolveSessionCookie(request);
			if (StringUtils.isNotBlank(sessionId)) {
				bearerType = BearerType.COOKIE_TYPE;
			}
		}

		doSessionAuthenticate(request, bearerType, sessionId, sessionManager);
	}

	private static String resolveSessionParameter(HttpServletRequest request) {
		String sessionId = request.getParameter(BearerType.SESSION_PARAM);
		if (StringUtils.isBlank(sessionId)) {
			sessionId = request.getParameter(BearerType.LEGACY_SESSION_PARAM);
		}
		return sessionId;
	}

	private static String resolveSessionCookie(HttpServletRequest request) {
		Cookie authCookie = WebContext.getCookie(request, BearerType.SESSION_COOKIE);
		if (authCookie == null) {
			authCookie = WebContext.getCookie(request, BearerType.LEGACY_SESSION_COOKIE);
		}
		return authCookie == null ? null : authCookie.getValue();
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
			log.debug("Session {} not found or expired.", sessionId);
			clearAuthentication(request);
			return;
		}

		sessionManager.refresh(sessionId);
		setAuthentication(request, session.getAuthentication());
		log.debug("Authenticated by session {}, type {}", sessionId, bearerType);
	}

	public static AuthAuthentication getAuthentication() {
		HttpServletRequest request = WebContext.getRequest();
		return request == null ? null : getAuthentication(request);
	}

	public static AuthAuthentication getAuthentication(HttpServletRequest request) {
		return (AuthAuthentication) request.getSession().getAttribute(WebConstants.AUTHENTICATION);
	}

	public static void setAuthentication(HttpServletRequest request, AuthAuthentication authentication) {
		if (request != null) {
			request.getSession().setAttribute(WebConstants.AUTHENTICATION, authentication);
		} else {
			setAuthentication(authentication);
		}
	}

	public static void setAuthentication(AuthAuthentication authentication) {
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
		AuthAuthentication authentication = getAuthentication();
		return authentication == null ? null : getPrincipal(authentication);
	}

	public static SignedPrincipal getPrincipal(AuthAuthentication authentication) {
		return authentication == null ? null : (SignedPrincipal) authentication.getPrincipal();
	}

	public static UserInfo getUserInfo(AuthAuthentication authentication) {
		SignedPrincipal principal = getPrincipal(authentication);
		return principal == null ? null : principal.getUserInfo();
	}

	public static UserInfo getUserInfo() {
		return getUserInfo(getAuthentication());
	}
}
