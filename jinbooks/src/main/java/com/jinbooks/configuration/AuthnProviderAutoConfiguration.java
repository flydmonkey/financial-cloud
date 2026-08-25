package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.jinbooks.authn.jwt.service.AuthTokenService;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.provider.AbstractAuthenticationProvider;
import com.jinbooks.authn.provider.AuthenticationProviderFactory;
import com.jinbooks.authn.provider.impl.NormalAuthenticationProvider;
import com.jinbooks.authn.realm.AbstractAuthenticationRealm;
import com.jinbooks.captcha.HutoolCaptchaService;
import com.jinbooks.service.auth.LoginService;

/**
 * 认证提供者自动配置，可根据需要增加新的提供者
 *
 * @author Crystal.Sea
 *
 */
@Slf4j
@Configuration
public class AuthnProviderAutoConfiguration {

    @Bean
	@Primary
    AbstractAuthenticationProvider authenticationProvider(
    		NormalAuthenticationProvider normalAuthenticationProvider) {
    	AuthenticationProviderFactory provider = new AuthenticationProviderFactory();
    	provider.addAuthenticationProvider(normalAuthenticationProvider);

    	return provider;
    }

    @Bean
    NormalAuthenticationProvider normalAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            LoginService loginService,
            AuthTokenService authTokenService,
            SessionManager sessionManager,
            HutoolCaptchaService hutoolCaptchaService) {
    	log.debug("init Normal authentication Provider .");
    	return new NormalAuthenticationProvider(
        		authenticationRealm,
        		loginService,
        		authTokenService,
        		sessionManager,
        		hutoolCaptchaService
        	);
    }

}
