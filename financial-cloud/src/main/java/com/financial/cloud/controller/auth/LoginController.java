package com.financial.cloud.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.authn.core.AuthAuthentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.LoginCredential;
import com.financial.cloud.authn.dto.LoginConfigDto;
import com.financial.cloud.authn.jwt.AuthJwt;
import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.provider.AbstractAuthenticationProvider;
import com.financial.cloud.configuration.LoginConfig;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

import org.springframework.http.MediaType;


/**
 * 登录
 * <p>
 * 登录界面初始化/login/get
 * </p>
 *
 * <p>
 * 登录入口/login/signin
 * </p>
 *
 * @author Crystal.Sea
 *
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value = "/api/login")
public class LoginController {

	private final AuthTokenService authTokenService;

	private final LoginConfig loginConfig;

	private final AbstractAuthenticationProvider authenticationProvider;

	private final LoginService loginService;

	/**
	 * init login。登录界面初始化信息
	 * @return
	 */
 	@GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<LoginConfigDto> get() {
		log.debug("/login.");
		LoginConfigDto conf = new LoginConfigDto();
		ConfigLoginPolicy loginPolicy = loginService.getConfigLoginPolicy();
		Institutions inst = (Institutions)WebContext.getAttribute(WebConstants.CURRENT_INST);
		conf.setInst(inst);
		conf.setCaptcha(loginPolicy.getCaptchaMgt().toUpperCase());
		conf.setState(authTokenService.genState());
		return new Message<>(conf);
	}

 	/**
 	 * 常规用户名和密码登录
 	 * @param credential
 	 * @return
 	 */
 	@PostMapping(value={"/signin"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<AuthJwt> signin( @RequestBody LoginCredential credential) {
 		Message<AuthJwt> authJwtMessage = new Message<>(Message.FAIL);
 		if(authTokenService.validateState(credential.getState())){
	 		AuthAuthentication authentication  = authenticationProvider.authenticate(credential);
	 		if(authentication != null) {//success
	 			AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
	 			authJwtMessage.setData(Message.SUCCESS,authJwt);
	 		}else {//fail
	 			String errorMsg = WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE) == null ?
							      "" : WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE).toString();
	 			authJwtMessage.setMessage(errorMsg);
	 			log.debug("login fail , message {}",errorMsg);
	 		}
 		}
 		return authJwtMessage;
 	}

}
