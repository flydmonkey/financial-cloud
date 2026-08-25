package com.jinbooks.configuration;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jinbooks.authn.provider.AbstractAuthenticationProvider;
import com.jinbooks.authn.interceptor.PermissionInterceptor;

@RequiredArgsConstructor
@Slf4j
@EnableWebMvc
@Configuration
public class JinBooksMvcConfig implements WebMvcConfigurer {
    
    private final AbstractAuthenticationProvider authenticationProvider;
    
    private final PermissionInterceptor permissionInterceptor;
    
    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.sameOrigin())
                        .cacheControl(Customizer.withDefaults()))
                .build();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.debug("add PermissionInterceptor default-deny");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/login/**",
                        "/api/captcha",
                        "/api/secretKey/**",
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
