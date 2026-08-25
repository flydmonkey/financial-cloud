package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.Filter;

import com.jinbooks.filter.WebHttpRestrictHostRequestFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jinbooks.configuration.ApplicationConfig;

/**
 * 请求域名限定自动装配
 */
@Slf4j
@Configuration
public class MvcRestrictHostAutoConfiguration implements WebMvcConfigurer {

    @Bean
    FilterRegistrationBean<Filter> webHttpRestrictHostRequestFilter(
                                                ApplicationConfig applicationConfig) {
        log.debug("WebHttpRestrictHostRequestFilter init for /* ");
        FilterRegistrationBean<Filter> registrationBean =
        		new FilterRegistrationBean<>(new WebHttpRestrictHostRequestFilter(applicationConfig.getRestrictHosts()));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("webHttpRestrictHostRequestFilter");
        registrationBean.setOrder(4);
        return registrationBean;
    }

}
