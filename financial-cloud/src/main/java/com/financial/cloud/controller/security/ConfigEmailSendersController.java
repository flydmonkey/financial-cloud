package com.financial.cloud.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.util.LegacySecretCodec;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.ConfigEmailSenders;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.security.ConfigEmailSendersService;

@RequiredArgsConstructor
@Slf4j
// DISABLED open-register-book-auth: menu hidden, code retained
//@RestController
@RequestMapping(value={"/api/security/emailsenders"})
public class ConfigEmailSendersController {

	private final ConfigEmailSendersService configEmailSendersService;

	private final LegacySecretCodec legacySecretCodec;

	@GetMapping(value={"/get"})
	public Message<ConfigEmailSenders> get(@CurrentUser UserInfo currentUser){
		ConfigEmailSenders emailSenders = configEmailSendersService.getById(currentUser.getBookId());
		if(emailSenders != null && StringUtils.isNotBlank(emailSenders.getCredentials())) {
			emailSenders.setCredentials(legacySecretCodec.decoder(emailSenders.getCredentials()));
		}else {
			emailSenders =new ConfigEmailSenders();
			emailSenders.setProtocol("smtp");
			emailSenders.setEncoding("utf-8");
		}
		return new Message<>(emailSenders);
	}

	@PutMapping(value={"/update"})
	public Message<ConfigEmailSenders> update( @RequestBody ConfigEmailSenders emailSenders,@CurrentUser UserInfo currentUser,BindingResult result) {
		log.debug("update emailSenders : {}",emailSenders);
		emailSenders.setBookId(currentUser.getBookId());
		emailSenders.setCredentials(legacySecretCodec.encode(emailSenders.getCredentials()));
		if(StringUtils.isBlank(emailSenders.getId())) {
			emailSenders.setId(emailSenders.getBookId());
			if(configEmailSendersService.save(emailSenders)) {
				return new Message<>(Message.SUCCESS);
			}else {
				return new Message<>(Message.ERROR);
			}
		}else {
			if(configEmailSendersService.updateById(emailSenders)) {
				return new Message<>(Message.SUCCESS);
			}else {
				return new Message<>(Message.ERROR);
			}
		}

	}
}
