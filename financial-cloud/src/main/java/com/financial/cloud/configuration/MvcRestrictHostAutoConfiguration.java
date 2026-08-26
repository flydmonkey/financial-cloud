package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.Filter;

import com.financial.cloud.filter.WebHttpRestrictHostRequestFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.financial.cloud.configuration.ApplicationConfig;

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
