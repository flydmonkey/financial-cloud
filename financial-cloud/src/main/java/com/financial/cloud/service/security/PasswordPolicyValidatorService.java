package com.financial.cloud.service.security;


import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.context.MessageSource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.financial.cloud.dto.auth.ChangePassword;
import com.financial.cloud.domain.security.ConfigPasswordPolicy;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.security.ConfigPasswordPolicyService;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;
import com.financial.cloud.context.WebContext;

@Slf4j
public class PasswordPolicyValidatorService{

    public static final String PASSWORD_POLICY_VALIDATE_RESULT = "PASSWORD_POLICY_VALIDATE_RESULT_KEY";

    public static final String PASSWORD_VALIDATOR_KEY = "PASSWORD_VALIDATOR_KEY";

    ConfigPasswordPolicy passwordPolicy;

    //Cache PasswordValidator in memory ONE_HOUR
    protected static final Cache<String, PasswordValidator> passwordValidatorStore =
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build();

    ConfigPasswordPolicyService configPasswordPolicyService;

    MessageSource messageSource;

    public PasswordPolicyValidatorService() {
    }

    public PasswordPolicyValidatorService(ConfigPasswordPolicyService configPasswordPolicyService,MessageSource messageSource) {
        this.messageSource = messageSource;
        this.configPasswordPolicyService = configPasswordPolicyService;
        passwordPolicy = configPasswordPolicyService.getPasswordPolicy();

    }

    /**
     * static validator .
     * @param changePassword
     * @return boolean
     */
   public boolean validator(ChangePassword changePassword) {
       String password = changePassword.getPassword();
       String username = changePassword.getUsername();
       if(StringUtils.isBlank(username)||StringUtils.isBlank(password)){
           log.debug("username or password  is Empty ");
           return false;
       }
       PasswordValidator passwordValidator = passwordValidatorStore.getIfPresent(PASSWORD_VALIDATOR_KEY);
       if(passwordValidator == null) {
    	   passwordPolicy = configPasswordPolicyService.getPasswordPolicy();
    	   passwordValidator = new PasswordValidator(
               new PasswordPolicyMessageResolver(messageSource),
               configPasswordPolicyService.getRuleList(passwordPolicy));
    	   passwordValidatorStore.put(PASSWORD_VALIDATOR_KEY, passwordValidator);
       }

       RuleResult result = passwordValidator.validate(new PasswordData(username,password));

       if (result.isValid()) {
           log.debug("Password is valid");
           return true;
       } else {
           log.debug("Invalid password:");
           StringBuilder passwordPolicyMessage = new StringBuilder("");
           for (String msg : passwordValidator.getMessages(result)) {
               passwordPolicyMessage.append(msg).append("<br>");
               log.debug("Rule Message {}" , msg);
           }
           WebContext.setAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT, passwordPolicyMessage);
           throw new BusinessException(400, String.valueOf(passwordPolicyMessage));
       }
   }

   public String generateRandomPassword() {
       StringBuilder chars = new StringBuilder();
       chars.append(RandomStringUtils.random(passwordPolicy.getLowerCase(), 'a', 'z'));
       chars.append(RandomStringUtils.random(passwordPolicy.getUpperCase(), 'A', 'Z'));
       chars.append(RandomStringUtils.random(passwordPolicy.getDigits(), '0', '9'));
       chars.append(RandomStringUtils.random(passwordPolicy.getSpecialChar(), "~@#^()[]*$-+?_&=!%{}/".toCharArray()));
       int remaining = passwordPolicy.getRandomPasswordLength() - chars.length();
       if (remaining > 0) {
           chars.append(RandomStringUtils.random(remaining, true, true));
       }
       return RandomStringUtils.random(passwordPolicy.getRandomPasswordLength(), chars.toString().toCharArray());
   }

}
