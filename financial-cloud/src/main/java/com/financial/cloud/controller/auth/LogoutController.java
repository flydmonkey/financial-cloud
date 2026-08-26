package com.financial.cloud.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.permissions.SessionListService;
import com.financial.cloud.util.AuthorizationHeaderUtils;

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
