package com.jinbooks.authn.realm.jdbc;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jinbooks.authn.realm.AbstractAuthenticationRealm;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.service.security.PasswordPolicyValidatorService;

/**
 * JdbcAuthenticationRealm.数据认证域
 *
 * @author Crystal.Sea
 *
 */
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
