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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jinbooks.authn.provider.AbstractAuthenticationProvider;
import com.jinbooks.authn.web.interceptor.PermissionInterceptor;

@EnableWebMvc
@AutoConfiguration
public class JinBooksMvcConfig implements WebMvcConfigurer {
    private static final  Logger logger = LoggerFactory.getLogger(JinBooksMvcConfig.class);
    
    @Autowired
    AbstractAuthenticationProvider authenticationProvider ;
    
    @Autowired
    PermissionInterceptor permissionInterceptor;
    
    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.debug("add PermissionInterceptor default-deny");
        permissionInterceptor.setMgmt(true);
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login/**",
                        "/captcha",
                        "/secretKey/**",
                        "/auth/token/refresh",
                        "/auth/entrypoint",
                        "/auth/refusedpoint",
                        "/open/func/list",
                        "/metadata/version",
                        "/actuator/health",
                        "/actuator/info",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/exception/error/**"
                );
        logger.debug("PermissionInterceptor registered");
    }
    
}
