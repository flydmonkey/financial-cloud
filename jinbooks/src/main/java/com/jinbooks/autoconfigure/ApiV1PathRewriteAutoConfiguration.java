package com.jinbooks.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.jinbooks.web.filter.ApiV1PathRewriteFilter;

@AutoConfiguration
public class ApiV1PathRewriteAutoConfiguration {

    @Bean
    public FilterRegistrationBean<ApiV1PathRewriteFilter> apiV1PathRewriteFilter() {
        FilterRegistrationBean<ApiV1PathRewriteFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiV1PathRewriteFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        bean.setName("apiV1PathRewriteFilter");
        return bean;
    }
}
