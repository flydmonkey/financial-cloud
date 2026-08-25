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

package com.jinbooks.controller.permissions;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.permissions.Resources;
import com.jinbooks.dto.auth.AppResourcesVo;
import com.jinbooks.service.auth.LoginService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = { "/open/func" })
public class OpenFuncListController {

	@Autowired
	SessionManager sessionManager;

	@Autowired
	LoginService loginService;
	@GetMapping(value = "/list")
	public Message<AppResourcesVo> getFunctionsList(@RequestParam("appId") String appId, HttpServletRequest request) {
		AuthorizationUtils.authenticate(request, sessionManager);
		UserInfo user = AuthorizationUtils.getUserInfo();
		if (user != null) {
			Set<Resources> functions = loginService.getResourcesBySubject(user);
			return new Message<>(new AppResourcesVo(functions));
		}
		return new Message<>(new AppResourcesVo(new HashSet<>()));
	}
}
