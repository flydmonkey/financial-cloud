package com.jinbooks.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.LoginCredential;
import com.jinbooks.authn.dto.LoginConfigDto;
import com.jinbooks.authn.jwt.AuthJwt;
import com.jinbooks.authn.jwt.service.AuthTokenService;
import com.jinbooks.authn.provider.AbstractAuthenticationProvider;
import com.jinbooks.configuration.ApplicationConfig;
import com.jinbooks.configuration.LoginConfig;
import com.jinbooks.domain.config.Institutions;
import com.jinbooks.common.Message;
import com.jinbooks.domain.security.ConfigLoginPolicy;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.context.WebConstants;
import com.jinbooks.context.WebContext;

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

	private final ApplicationConfig applicationConfig;

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
	 		Authentication authentication  = authenticationProvider.authenticate(credential);
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
