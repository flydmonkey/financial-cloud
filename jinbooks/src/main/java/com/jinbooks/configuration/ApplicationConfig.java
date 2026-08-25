package com.jinbooks.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局应用程序配置，包含数据源、字符集、可访问地址等运行时参数。
 */

@Data
@NoArgsConstructor
@Component
@Configuration
public class ApplicationConfig {

    @Value("${jinbooks.server.frontend.uri:https://www.jinbooks.com:4200}")
    private String frontendUri;

    @Value("${server.port:8080}")
    private int port;

    @Value("#{'${jinbooks.server.restrict.hosts:}'.split(',')}")
	List<String> restrictHosts;
    
    @Value("${jinbooks.job.cron.schedule:0 0 0/1 * * ?}") 
    String jobCronSchedule;
    
    @Value("${jinbooks.job.session.listener:false}") 
    boolean isJobSessionListener;

}
