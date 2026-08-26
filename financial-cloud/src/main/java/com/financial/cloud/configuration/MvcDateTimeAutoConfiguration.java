package com.financial.cloud.configuration;


import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * MVC 请求数据日期格式转换
 */
@Slf4j
@Configuration
public class MvcDateTimeAutoConfiguration extends WebMvcConfigurationSupport {

	 @Value("${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}")
	 private String pattern;

	@Bean
    @Override
    public FormattingConversionService mvcConversionService() {
		log.debug("DateTimeFormatter Pattern {}",pattern);
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService(false);

        DateTimeFormatterRegistrar dateTimeRegistrar = new DateTimeFormatterRegistrar();
        dateTimeRegistrar.setDateFormatter(DateTimeFormatter.ofPattern(pattern));
        dateTimeRegistrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(pattern));
        dateTimeRegistrar.registerFormatters(conversionService);

        DateFormatterRegistrar dateRegistrar = new DateFormatterRegistrar();
        dateRegistrar.setFormatter(new DateFormatter(pattern));
        dateRegistrar.registerFormatters(conversionService);

        return conversionService;
    }
}
