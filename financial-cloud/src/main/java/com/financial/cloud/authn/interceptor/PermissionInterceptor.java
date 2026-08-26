package com.financial.cloud.authn.interceptor;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.authn.core.Authority;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.LoginService;

import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Slf4j
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

	private final SessionManager sessionManager;

	private final LoginService loginService;

	private final JsonMapper jsonMapper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		log.trace("Permission Interceptor .");
		AuthorizationUtils.authenticate(request, sessionManager);
		SignedPrincipal principal = AuthorizationUtils.getPrincipal();
		if (principal == null) {
			log.trace("No Authentication for URI {}", request.getRequestURI());
			writeUnauthorized(response);
			return false;
		}
		UserInfo userInfo = principal.getUserInfo();
		if (userInfo != null) {
			List<Authority> authorities = loginService.grantAuthority(userInfo);
			principal.setAuthenticated(true);
			AuthAuthentication authenticationToken =
					AuthAuthentication.authenticated(principal, null, authorities);
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
