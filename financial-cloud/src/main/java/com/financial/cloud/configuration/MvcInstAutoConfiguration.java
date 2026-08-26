package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.Filter;

import com.financial.cloud.filter.WebHttpInstRequestFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.financial.cloud.service.config.InstitutionsService;

@Slf4j
@Configuration
public class MvcInstAutoConfiguration implements WebMvcConfigurer {

    @Bean
    FilterRegistrationBean<Filter> webHttpInstRequestFilter(InstitutionsService institutionsService) {
        log.debug("WebHttpInstRequestFilter init for /* ");
        FilterRegistrationBean<Filter> registrationBean =
        		new FilterRegistrationBean<>(new WebHttpInstRequestFilter(institutionsService));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("webHttpInstRequestFilter");
        registrationBean.setOrder(5);
        return registrationBean;
    }

}
