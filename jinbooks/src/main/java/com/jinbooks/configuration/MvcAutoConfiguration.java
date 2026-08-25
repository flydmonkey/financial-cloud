package com.jinbooks.configuration;


import lombok.extern.slf4j.Slf4j;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.jinbooks.util.JsonUtils;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;


@Slf4j
@Configuration
public class MvcAutoConfiguration implements WebMvcConfigurer {

    @Value("${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}")
    private String pattern;

    /**
     * 消息处理，可以直接使用properties的key值，返回的是对应的value值
     * messageSource .
     * @return messageSource
     */
    @Bean(name = "messageSource")
    ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource(
            @Value("${spring.messages.basename:classpath:messages/messages}")
            String messagesBasename)  {
        log.debug("Basename {}" , messagesBasename);

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(messagesBasename);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(false);
        return messageSource;
    }

    /**
     * Locale Change Interceptor and Resolver definition .
     * @return localeChangeInterceptor
     */
    //@Primary
    @Bean(name = "localeChangeInterceptor")
    LocaleChangeInterceptor localeChangeInterceptor()  {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("language");
        return localeChangeInterceptor;
    }

    /**
     * handlerMapping .
     * @return handlerMapping
     */
    @Bean(name = "handlerMapping")
    RequestMappingHandlerMapping requestMappingHandlerMapping(LocaleChangeInterceptor localeChangeInterceptor) {
        RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
        requestMappingHandlerMapping.setInterceptors(localeChangeInterceptor);
        return requestMappingHandlerMapping;
    }



    @Bean
    JsonMapper jsonMapper() {
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultDateFormat(new SimpleDateFormat(pattern))
                .build();
        JsonUtils.setMapper(mapper);
        log.debug("JsonMapper DateFormat {}" , pattern);
        return mapper;
    }

    /**
     * jacksonJsonHttpMessageConverter .
     * @return jacksonJsonHttpMessageConverter
     */
    @Bean(name = "mappingJacksonHttpMessageConverter")
    JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
        ArrayList<MediaType> mediaTypesList = new ArrayList<>();
        mediaTypesList.add(MediaType.APPLICATION_JSON);
        mediaTypesList.add(MediaType.valueOf(ApiVersion.V2.getProducedMimeType().toString()));
        mediaTypesList.add(MediaType.valueOf(ApiVersion.V3.getProducedMimeType().toString()));
        log.debug("jacksonJsonHttpMessageConverter MediaTypes {}" , mediaTypesList);
        converter.setSupportedMediaTypes(mediaTypesList);
        return converter;
    }

    /**
     * localeResolver .
     * @return localeResolver
     */

    @Bean(name = "localeResolver")
    LocaleResolver localeResolver(
            @Value("${jinbooks.server.domain:jinbooks.top}")
            String domainName) {
        log.debug("DomainName {}" , domainName);
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver("jb_locale");
        cookieLocaleResolver.setCookieDomain(domainName);
        //2 week
        cookieLocaleResolver.setCookieMaxAge(Duration.ofDays(14));
        return cookieLocaleResolver;
    }

    /**
     * AnnotationMethodHandlerAdapter
     * requestMappingHandlerAdapter .
     * @return requestMappingHandlerAdapter
     */
    @Bean(name = "addConverterRequestMappingHandlerAdapter")
    RequestMappingHandlerAdapter requestMappingHandlerAdapter(
            JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter,
            RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        List<HttpMessageConverter<?>> httpMessageConverterList = new ArrayList<>();
        // ByteArray responses need a dedicated converter before JSON.
        httpMessageConverterList.add(new ByteArrayHttpMessageConverter());
        httpMessageConverterList.add(jacksonJsonHttpMessageConverter);
        httpMessageConverterList.add(stringHttpMessageConverter);
        log.debug("stringHttpMessageConverter {}",stringHttpMessageConverter.getDefaultCharset());

        requestMappingHandlerAdapter.setMessageConverters(httpMessageConverterList);
        return requestMappingHandlerAdapter;
    }

    /**
     * restTemplate .
     * @return restTemplate
     */
    @Bean(name = "restTemplate")
    RestTemplate restTemplate(JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter) {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> httpMessageConverterList = new ArrayList<>();
        httpMessageConverterList.add(jacksonJsonHttpMessageConverter);
        restTemplate.setMessageConverters(httpMessageConverterList);
        return restTemplate;
    }

    /**
     * 配置默认错误页面（仅用于内嵌tomcat启动时） 使用这种方式，在打包为war后不起作用.
     *
     * @return webServerFactoryCustomizer
     */
    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        return new WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>() {
            @Override
            public void customize(ConfigurableServletWebServerFactory factory) {
                log.debug("WebServerFactoryCustomizer ... ");
                ErrorPage errorPage400 =
                        new ErrorPage(HttpStatus.BAD_REQUEST, "/api/exception/error/400");
                ErrorPage errorPage404 =
                        new ErrorPage(HttpStatus.NOT_FOUND, "/api/exception/error/404");
                ErrorPage errorPage500 =
                        new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/api/exception/error/500");
                factory.addErrorPages(errorPage400, errorPage404, errorPage500);
            }
        };
    }

    @Bean
    SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter() {
        log.debug("securityContextHolderAwareRequestFilter init ");
        return new SecurityContextHolderAwareRequestFilter();
    }

    @Bean
    FilterRegistrationBean<Filter> delegatingFilterProxy() {
        log.debug("delegatingFilterProxy init for /* ");
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DelegatingFilterProxy("securityContextHolderAwareRequestFilter"));
        registrationBean.addUrlPatterns("/*");
        //registrationBean.
        registrationBean.setName("delegatingFilterProxy");
        registrationBean.setOrder(2);

        return registrationBean;
    }

}
