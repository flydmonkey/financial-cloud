package com.jinbooks.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.util.LegacySecretCodec;
import com.jinbooks.common.Message;
import com.jinbooks.domain.security.ConfigSmsProvider;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.security.ConfigSmsProviderService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/security/smsprovider"})
public class ConfigSmsProviderController {

	private final ConfigSmsProviderService configSmsProviderService;

	@GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<ConfigSmsProvider> get(@CurrentUser UserInfo currentUser){
		ConfigSmsProvider smsProvider = configSmsProviderService.getById(currentUser.getBookId());
		if(smsProvider != null && StringUtils.isNoneBlank(smsProvider.getId())) {
			smsProvider.setAppSecret(LegacySecretCodec.getInstance().decoder(smsProvider.getAppSecret()));
		}
		return new Message<>(smsProvider);
	}

	@PutMapping(value={"/update"})
	public Message<ConfigSmsProvider> update( @RequestBody ConfigSmsProvider smsProvider,@CurrentUser UserInfo currentUser,BindingResult result) {
		log.debug("update smsProvider : {}" ,smsProvider);
		smsProvider.setAppSecret(LegacySecretCodec.getInstance().encode(smsProvider.getAppSecret()));
		smsProvider.setBookId(currentUser.getBookId());
		boolean updateResult = false;
		if(StringUtils.isBlank(smsProvider.getId())) {
			smsProvider.setId(smsProvider.getBookId());
			updateResult = configSmsProviderService.save(smsProvider);
		}else {
			updateResult = configSmsProviderService.updateById(smsProvider);
		}
		if(updateResult) {
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}
}
