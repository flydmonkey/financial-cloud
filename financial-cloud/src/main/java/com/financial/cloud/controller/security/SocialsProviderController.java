package com.financial.cloud.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.util.LegacySecretCodec;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.SocialsProvider;
import com.financial.cloud.dto.security.SocialsProviderPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.security.SocialsProviderService;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@Slf4j
// DISABLED open-register-book-auth: menu hidden, code retained
//@RestController
@RequestMapping(value={"/api/security/socialsprovider"})
public class SocialsProviderController {

	private final SocialsProviderService socialsProviderService;

	private final LegacySecretCodec legacySecretCodec;

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<SocialsProvider>> fetch(SocialsProviderPageDto dto, @CurrentUser UserInfo currentUser) {
		log.debug("fetch {}",dto);

		LambdaQueryWrapper<SocialsProvider> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SocialsProvider::getBookId, currentUser.getBookId());

		return new Message<>(Message.SUCCESS, socialsProviderService.page(dto.build(), wrapper));
	}

	@GetMapping(value={"/query"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<SocialsProvider> query(SocialsProviderPageDto dto,@CurrentUser UserInfo currentUser) {
		log.debug("-query  : {}" , dto);
		LambdaQueryWrapper<SocialsProvider> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SocialsProvider::getBookId, currentUser.getBookId());
		if (ObjectUtils.isNotEmpty(socialsProviderService.list(wrapper))) {
			 return new Message<>(Message.SUCCESS);
		} else {
			 return new Message<>(Message.FAIL);
		}
	}

	@GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<SocialsProvider> get(@PathVariable("id") String id) {
		SocialsProvider socialsProvider=socialsProviderService.getById(id);
		socialsProvider.setClientSecret(legacySecretCodec.decoder(socialsProvider.getClientSecret()));
		return new Message<>(socialsProvider);
	}

	@PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<SocialsProvider> insert(@RequestBody  SocialsProvider socialsProvider,@CurrentUser UserInfo currentUser) {
		log.debug("-Add  : {}" , socialsProvider);
		socialsProvider.setBookId(currentUser.getBookId());
		socialsProvider.setClientSecret(legacySecretCodec.encode(socialsProvider.getClientSecret()));
		if (socialsProviderService.save(socialsProvider)) {
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<SocialsProvider> update(@RequestBody  SocialsProvider socialsProvider,@CurrentUser UserInfo currentUser) {
		log.debug("-update  : {}" , socialsProvider);
		socialsProvider.setBookId(currentUser.getBookId());
		socialsProvider.setClientSecret(legacySecretCodec.encode(socialsProvider.getClientSecret()));
		if (socialsProviderService.updateById(socialsProvider)) {
		    return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<SocialsProvider> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		log.debug("-delete  ids : {} " , ids);
		if (socialsProviderService.removeByIds(ids)) {
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

}
