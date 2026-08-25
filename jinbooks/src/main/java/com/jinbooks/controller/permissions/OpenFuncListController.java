package com.jinbooks.controller.permissions;


import lombok.RequiredArgsConstructor;
import java.util.HashSet;
import java.util.Set;

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
