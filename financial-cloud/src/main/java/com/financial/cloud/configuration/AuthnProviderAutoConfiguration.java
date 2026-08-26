package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.provider.AbstractAuthenticationProvider;
import com.financial.cloud.authn.provider.AuthenticationProviderFactory;
import com.financial.cloud.authn.provider.impl.NormalAuthenticationProvider;
import com.financial.cloud.authn.realm.AbstractAuthenticationRealm;
import com.financial.cloud.captcha.HutoolCaptchaService;
import com.financial.cloud.service.auth.LoginService;

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
