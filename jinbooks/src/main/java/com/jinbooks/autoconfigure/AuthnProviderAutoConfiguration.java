/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 





package com.jinbooks.autoconfigure;

import com.jinbooks.ip2location.IpLocationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@AutoConfiguration
public class AuthnProviderAutoConfiguration {
    static final  Logger logger = LoggerFactory.getLogger(AuthnProviderAutoConfiguration.class);

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
            IpLocationParser ipLocationParser,
            LoginService loginService,
            AuthTokenService authTokenService,
            SessionManager sessionManager,
            HutoolCaptchaService hutoolCaptchaService) {
    	logger.debug("init Normal authentication Provider .");
    	return new NormalAuthenticationProvider(
        		authenticationRealm,
        		loginService,
        		authTokenService,
        		sessionManager,
        		ipLocationParser,
        		hutoolCaptchaService
        	);
    }

}
