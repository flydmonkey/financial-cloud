package com.financial.cloud.authn.realm.jdbc;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.financial.cloud.authn.realm.AbstractAuthenticationRealm;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;

@Slf4j
public class JdbcAuthenticationRealm extends AbstractAuthenticationRealm {

    protected PasswordEncoder passwordEncoder;

    public JdbcAuthenticationRealm() {
        log.debug("init . ");
    }


    public JdbcAuthenticationRealm(
    		PasswordEncoder passwordEncoder,
    		PasswordPolicyValidatorService passwordPolicyValidator,
    		LoginService loginService) {

    	this.passwordEncoder = passwordEncoder;
    	this.passwordPolicyValidator = passwordPolicyValidator;
    	this.loginService = loginService;
    }


    /**
     * passwordMatches.
     */
    public boolean passwordMatches(UserInfo userInfo, String password) {
        boolean passwordMatches = false;
        //jdbc password check
        passwordMatches = passwordEncoder.matches(password,userInfo.getPassword());

        log.debug("passwordvalid : {}" , passwordMatches);

        return passwordMatches;
    }

}
