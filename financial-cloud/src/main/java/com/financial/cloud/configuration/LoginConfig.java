package com.financial.cloud.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Configuration
public class LoginConfig {
    
    @Value("${financial-cloud.login.jwt.issuer:https://www.financial-cloud.com}")
    String jwtIssuer;
    
    @Value("${financial-cloud.login.cas.serverUrlPrefix:https://www.financial-cloud.com/sign/authz/cas}")
    String casServerUrlPrefix;
    
    @Value("${financial-cloud.login.cas.service:https://www.financial-cloud.com/passport/trust/auth}")
    String casService;
}
