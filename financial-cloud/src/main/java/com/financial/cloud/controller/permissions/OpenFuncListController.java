package com.financial.cloud.controller.permissions;


import lombok.RequiredArgsConstructor;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.dto.auth.AppResourcesVo;
import com.financial.cloud.service.auth.LoginService;

import jakarta.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = { "/api/open/func" })
public class OpenFuncListController {

	private final SessionManager sessionManager;

	private final LoginService loginService;
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
