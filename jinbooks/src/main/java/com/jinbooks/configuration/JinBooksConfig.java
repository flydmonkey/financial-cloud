package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jinbooks.authn.realm.jdbc.JdbcAuthenticationRealm;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;

@Slf4j
@Configuration
@EnableScheduling
public class JinBooksConfig {

    @Bean
    JdbcAuthenticationRealm authenticationRealm(
            @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder,
            PasswordPolicyValidatorService passwordPolicyValidator,
            LoginService loginService) {

        JdbcAuthenticationRealm authenticationRealm = new JdbcAuthenticationRealm(
        		passwordEncoder,
        		passwordPolicyValidator,
        		loginService);

        log.debug("JdbcAuthenticationRealm inited.");
        return authenticationRealm;
    }

}
