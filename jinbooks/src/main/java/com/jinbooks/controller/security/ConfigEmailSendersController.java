package com.jinbooks.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.util.LegacySecretCodec;
import com.jinbooks.common.Message;
import com.jinbooks.domain.security.ConfigEmailSenders;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.security.ConfigEmailSendersService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/security/emailsenders"})
public class ConfigEmailSendersController {

	private final ConfigEmailSendersService configEmailSendersService;

	@GetMapping(value={"/get"})
	public Message<ConfigEmailSenders> get(@CurrentUser UserInfo currentUser){
		ConfigEmailSenders emailSenders = configEmailSendersService.getById(currentUser.getBookId());
		if(emailSenders != null && StringUtils.isNotBlank(emailSenders.getCredentials())) {
			emailSenders.setCredentials(LegacySecretCodec.getInstance().decoder(emailSenders.getCredentials()));
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
		emailSenders.setCredentials(LegacySecretCodec.getInstance().encode(emailSenders.getCredentials()));
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
