package com.jinbooks.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.permissions.SessionListService;
import com.jinbooks.util.AuthorizationHeaderUtils;

/**
 * 前端注销
 */
@RequiredArgsConstructor
@Slf4j
@RestController
public class LogoutController {

	private final SessionManager sessionManager;

	private final SessionListService sessionListService;

	@GetMapping(value = { "/api/logout" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Message<String> logout(
			HttpServletRequest request,
			@CurrentUser UserInfo currentUser) {
		log.debug("session {} user {}({}) logout",
				currentUser.getSessionId(), currentUser.getUsername(), currentUser.getId());
		String sessionId = AuthorizationHeaderUtils.resolveBearer(request);
		if (StringUtils.isBlank(sessionId) && currentUser != null) {
			sessionId = currentUser.getSessionId();
		}
		if (StringUtils.isNotBlank(sessionId)) {
			sessionManager.remove(sessionId);
			sessionListService.removeById(sessionId);
		}
		AuthorizationUtils.clearAuthentication(request);
		return new Message<>();
	}
}
