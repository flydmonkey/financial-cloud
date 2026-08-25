package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.Filter;

import com.jinbooks.filter.WebHttpInstRequestFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jinbooks.configuration.ApplicationConfig;
import com.jinbooks.service.config.InstitutionsService;

/**
 * 多租户机构读取的自动装配
 */
@Slf4j
@Configuration
public class MvcInstAutoConfiguration implements WebMvcConfigurer {

    @Bean
    FilterRegistrationBean<Filter> webHttpInstRequestFilter(
                                                InstitutionsService institutionsService,
                                                ApplicationConfig applicationConfig) {
        log.debug("WebHttpInstRequestFilter init for /* ");
        FilterRegistrationBean<Filter> registrationBean =
        		new FilterRegistrationBean<>(new WebHttpInstRequestFilter(institutionsService,applicationConfig));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("webHttpInstRequestFilter");
        registrationBean.setOrder(5);
        return registrationBean;
    }

}
