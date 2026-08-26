package com.financial.cloud.configuration;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.financial.cloud.authn.interceptor.PermissionInterceptor;

@RequiredArgsConstructor
@Slf4j
@EnableWebMvc
@Configuration
public class FinancialCloudMvcConfig implements WebMvcConfigurer {
    
    private final PermissionInterceptor permissionInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.debug("add PermissionInterceptor default-deny");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/login/**",
                        "/api/captcha",
                        "/api/auth/token/refresh",
                        "/api/auth/entrypoint",
                        "/api/auth/refusedpoint",
                        "/api/open/func/list",
                        "/api/metadata/version",
                        "/actuator/health",
                        "/actuator/info",
                        "/api/exception/error/**",
                        "/static/**",
                        "/error"
                );
        log.debug("PermissionInterceptor registered");
    }
    
}
