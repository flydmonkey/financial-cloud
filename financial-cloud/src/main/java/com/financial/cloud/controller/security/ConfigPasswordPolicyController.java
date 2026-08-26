package com.financial.cloud.controller.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.annotation.OrderBy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.ConfigPasswordPolicy;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.security.ConfigPasswordPolicyService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/security/passwordpolicy"})
public class ConfigPasswordPolicyController {

	private final ConfigPasswordPolicyService configPasswordPolicyService;

	@GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<ConfigPasswordPolicy> get(@CurrentUser UserInfo currentUser){
		return new Message<>(configPasswordPolicyService.getPasswordPolicy());
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<ConfigPasswordPolicy> update(@Valid @RequestBody ConfigPasswordPolicy passwordPolicy,@CurrentUser UserInfo currentUser,BindingResult result) {
		log.debug("updateRole passwordPolicy : {}" ,passwordPolicy);
		//Message message = this.validate(result, passwordPolicy);

		if(configPasswordPolicyService.updateById(passwordPolicy)) {
			return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.ERROR);
		}
	}

	public Message<BindingResult> validate(BindingResult result,ConfigPasswordPolicy passwordPolicy) {
		if (result.hasErrors()) {
			return new Message<>(result);
		}
		if(passwordPolicy.getMinLength() < 3) {
			FieldError fe = new FieldError("passwordPolicy", "minLength",
					passwordPolicy.getMinLength(), true,
					new String[]{"ui.passwordpolicy.xe00000001"},//密码最小长度不能小于3位字符
					null, null);
			result.addError(fe);
			return new Message<>(result);
		}
		if(passwordPolicy.getMinLength() > passwordPolicy.getMaxLength()) {
			FieldError fe = new FieldError("passwordPolicy", "maxLength",
					passwordPolicy.getMinLength(), true,
					new String[]{"ui.passwordpolicy.xe00000002"},//密码最大长度不能小于最小长度
					null, null);
			result.addError(fe);
			return new Message<>(result);
		}

		if(passwordPolicy.getDigits() + passwordPolicy.getLowerCase() + passwordPolicy.getUpperCase() + passwordPolicy.getSpecialChar() < 2) {
			FieldError fe = new FieldError("passwordPolicy", "specialChar",
					2, true,
					new String[]{"ui.passwordpolicy.xe00000003"},//密码包含小写字母、大写字母、数字、特殊字符的个数不能小于2
					null, null);
			result.addError(fe);
			return new Message<>(result);
		}

		if(passwordPolicy.getDigits() + passwordPolicy.getLowerCase() + passwordPolicy.getUpperCase() + passwordPolicy.getSpecialChar() > passwordPolicy.getMaxLength()) {
			FieldError fe = new FieldError("passwordPolicy", "specialChar",
					passwordPolicy.getMinLength(), true,
					new String[]{"ui.passwordpolicy.xe00000004"},//密码包含小写字母、大写字母、数字、特殊字符的个数不能大于密码的最大长度
					null, null);
			result.addError(fe);
			return new Message<>(result);
		}
		return null;
	}
}
