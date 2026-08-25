package com.jinbooks.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Configuration
public class LoginConfig {
    
    @Value("${jinbooks.login.jwt.issuer:https://www.jinbooks.com}")
    String jwtIssuer;
    
    @Value("${jinbooks.login.cas.serverUrlPrefix:https://www.jinbooks.com/sign/authz/cas}")
    String casServerUrlPrefix;
    
    @Value("${jinbooks.login.cas.service:https://www.jinbooks.com/passport/trust/auth}")
    String casService;
}
