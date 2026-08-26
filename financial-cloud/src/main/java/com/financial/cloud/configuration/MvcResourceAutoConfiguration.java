package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class MvcResourceAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	log.debug("add Resource Handlers");

        log.debug("add statics");
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        log.debug("add templates");
        registry.addResourceHandler("/templates/**")
                .addResourceLocations("classpath:/templates/");

        log.debug("add Resource Handler finished .");
    }
}
