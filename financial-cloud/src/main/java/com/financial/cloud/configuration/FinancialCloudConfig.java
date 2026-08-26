package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.financial.cloud.authn.realm.jdbc.JdbcAuthenticationRealm;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;

@Slf4j
@Configuration
@EnableScheduling
public class FinancialCloudConfig {

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
