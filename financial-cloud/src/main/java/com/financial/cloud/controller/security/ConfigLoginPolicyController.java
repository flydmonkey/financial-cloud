package com.financial.cloud.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.security.ConfigLoginPolicyService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/security/configLoginPolicy"})
public class ConfigLoginPolicyController {

	private final ConfigLoginPolicyService configLoginPolicyService;

	@GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<ConfigLoginPolicy> get(@CurrentUser UserInfo currentUser){
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getById(currentUser.getBookId());
		return new Message<>(configLoginPolicy);
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<ConfigLoginPolicy> update(@Valid @RequestBody ConfigLoginPolicy configLoginPolicy,@CurrentUser UserInfo currentUser) {
		log.debug("updateRole configLoginPolicy : {}" ,configLoginPolicy);
		if(configLoginPolicyService.updateById(configLoginPolicy)) {
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.ERROR);
		}
	}

}
