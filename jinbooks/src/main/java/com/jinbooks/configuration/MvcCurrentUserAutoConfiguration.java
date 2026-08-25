package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jinbooks.authn.resolver.CurrentUserMethodArgumentResolver;

@Slf4j
@EnableWebMvc
@Configuration
public class MvcCurrentUserAutoConfiguration  implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentUserMethodArgumentResolver());
        log.debug("add currentUserMethodArgumentResolver");
    }

    @Bean
    CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
        return new CurrentUserMethodArgumentResolver();
    }

}
