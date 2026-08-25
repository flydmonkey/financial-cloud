package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import com.jinbooks.filter.WebHttpXssRequestFilter;
import jakarta.servlet.Filter;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * XSS请求
 */
@Slf4j
@Configuration
public class MvcXssAutoConfiguration implements WebMvcConfigurer {

    @Bean
    FilterRegistrationBean<Filter> webHttpXssRequestFilter() {
        log.debug("WebHttpXssRequestFilter init for /* ");
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>(new WebHttpXssRequestFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("webHttpXssRequestFilter");
        registrationBean.setOrder(3);
        return registrationBean;
    }

}
