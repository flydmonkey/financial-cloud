package com.financial.cloud.authn.provider.impl;


import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.authn.core.AuthenticationException;
import com.financial.cloud.authn.core.BadCredentialsException;

import com.financial.cloud.authn.LoginCredential;
import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.provider.AbstractAuthenticationProvider;
import com.financial.cloud.authn.realm.AbstractAuthenticationRealm;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.captcha.HutoolCaptchaService;
import com.financial.cloud.constants.auth.ConstsCaptchaType;
import com.financial.cloud.constants.auth.ConstsLoginType;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.common.client.ClientResolve;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

/**
 * Normal Authentication provider.正常用户名密码认证提供者
 *
 * @author Crystal.Sea
 *
 */
@Slf4j
public class NormalAuthenticationProvider extends AbstractAuthenticationProvider {

    private HutoolCaptchaService hutoolCaptchaService;

    public String getProviderName() {
        return "normal" + PROVIDER_SUFFIX;
    }


    public NormalAuthenticationProvider() {
		super();
	}

    public NormalAuthenticationProvider(
    		AbstractAuthenticationRealm authenticationRealm,
    	    LoginService loginService,
    	    AuthTokenService authTokenService,
    	    SessionManager sessionManager,
    	    HutoolCaptchaService hutoolCaptchaService) {
		this.authenticationRealm = authenticationRealm;
		this.loginService = loginService;
		this.authTokenService = authTokenService;
		this.sessionManager = sessionManager;
		this.hutoolCaptchaService = hutoolCaptchaService;
	}

    @Override
	public AuthAuthentication doAuthenticate(LoginCredential loginCredential) {
		AuthAuthentication authenticationToken = null;
		loginCredential.setStyle(Session.STYLE.MGMT);
		log.debug("Trying to authenticate user {} via {}", loginCredential.getPrincipal(), getProviderName());
        try {
	        //判断图片验证码并验证
	        if(!this.loginService.getConfigLoginPolicy().getCaptchaMgt().equalsIgnoreCase(ConstsCaptchaType.NONE)) {
	        	captchaValid(loginCredential.getState(),loginCredential.getCaptcha());
	        }

	        emptyPasswordValid(loginCredential.getPassword());
	        emptyUsernameValid(loginCredential.getUsername());

	        //查询用户
	        UserInfo userInfo =  loadUserInfo(loginCredential.getUsername(),loginCredential.getPassword());
	        //获取登录终端信息
	        ClientResolve client = parserClientResolve();
	        statusValid(loginCredential , userInfo , client);
	        //Validate PasswordPolicy
	        authenticationRealm.applyLoginPolicy(userInfo);

	        //Match password 验证密码
	        boolean passwordMatches = authenticationRealm.passwordMatches(userInfo, loginCredential.getPassword());

	        if (!passwordMatches) {
	        	ConfigLoginPolicy configLoginPolicy = loginService.getConfigLoginPolicy();
	        	loginService.updateBadPasswordCount(userInfo);
	        	authenticationRealm.insertLoginHistory(userInfo,client, ConstsLoginType.NORMAL, "", "xe00000004", WebConstants.LOGIN_RESULT.PASSWORD_ERROE);
	            if(userInfo.getBadPasswordCount()>=(configLoginPolicy.getPasswordAttempts())) {
	                throw new BadCredentialsException(
	                        WebContext.getI18nValue(MessageKeys.Login.ERROR_PASSWORD_ATTEMPTS,
	                                new Object[]{
	                                        userInfo.getBadPasswordCount() ,
	                                        configLoginPolicy.getLoginAttempts(),
	                                        configLoginPolicy.getLockInterval()}));
	            }else {
	                throw new BadCredentialsException(WebContext.getI18nValue(MessageKeys.Login.ERROR_PASSWORD));
	            }
	        }

	        authenticationToken = createOnlineTicket(loginCredential,userInfo,client);
	        // user authenticated
	        log.debug("'{}' authenticated successfully by {}.",
	        		loginCredential.getPrincipal(), getProviderName());

	        authenticationRealm.insertLoginHistory(userInfo,
	        										client,
							        				ConstsLoginType.NORMAL,
									                "",
									                "xe00000004",
									                WebConstants.LOGIN_RESULT.SUCCESS);
        } catch (AuthenticationException e) {
            log.error("Failed to authenticate user {} via {}: {}",
                    				loginCredential.getPrincipal(),
                                    getProviderName(),
                                    e.getMessage() );
            WebContext.setAttribute(
                    WebConstants.LOGIN_ERROR_SESSION_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Login error Unexpected exception in {} authentication:{}" ,
                            getProviderName(), e.getMessage());
        }

        return  authenticationToken;
    }

    /**
     * captcha validate .图片验证码校验
     *
     * @param authType String
     * @param captcha String
     * @throws ParseException
     */
    protected void captchaValid(String state ,String captcha) {
        // for basic
    	if(!hutoolCaptchaService.validate(state, captcha)) {
    		throw new BadCredentialsException(WebContext.getI18nValue(MessageKeys.Login.ERROR_CAPTCHA));
    	}
    }
}
