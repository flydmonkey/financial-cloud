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

package com.jinbooks.controller.auth;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.util.AuthorizationHeaderUtils;

/**
 * 前端注销
 */
@RestController
public class LogoutController {
	private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

	@Autowired
	SessionManager sessionManager;

	@GetMapping(value = { "/logout" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Message<String> logout(
			HttpServletRequest request,
			@CurrentUser UserInfo currentUser) {
		logger.debug("session {} user {}({}) logout",
				currentUser.getSessionId(), currentUser.getUsername(), currentUser.getId());
		String sessionId = AuthorizationHeaderUtils.resolveBearer(request);
		if (StringUtils.isBlank(sessionId) && currentUser != null) {
			sessionId = currentUser.getSessionId();
		}
		if (StringUtils.isNotBlank(sessionId)) {
			sessionManager.remove(sessionId);
		}
		AuthorizationUtils.clearAuthentication(request);
		return new Message<>();
	}
}
