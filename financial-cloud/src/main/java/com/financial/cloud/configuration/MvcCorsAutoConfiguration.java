package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class MvcCorsAutoConfiguration implements WebMvcConfigurer {

    @Bean
    CorsFilter corsFilter() {
    	log.debug("CorsConfiguration init for /* ");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList(CorsConfiguration.ALL));
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        corsConfiguration.addAllowedMethod(HttpMethod.GET.name());
        corsConfiguration.addAllowedMethod(HttpMethod.POST.name());
        corsConfiguration.addAllowedMethod(HttpMethod.PUT.name());
        corsConfiguration.addAllowedMethod(HttpMethod.DELETE.name());
        corsConfiguration.addAllowedMethod(HttpMethod.HEAD.name());
        corsConfiguration.addAllowedMethod(HttpMethod.PATCH.name());
        log.debug("CorsConfiguration AllowedMethods {} ",corsConfiguration.getAllowedMethods());
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(1);
        bean.addUrlPatterns("/*");
        return bean;
    }

}
