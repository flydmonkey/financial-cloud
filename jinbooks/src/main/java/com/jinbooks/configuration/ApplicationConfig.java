package com.jinbooks.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局应用程序配置 包含 1、数据源配置 dataSoruceConfig 2、字符集转换配置 characterEncodingConfig
 * 3、webseal认证集成配置 webSealConfig 4、系统的配置 sysConfig 5、所有用户可访问地址配置 allAccessUrl
 * 
 * 其中1、2、3项在applicationContext.xml中配置，配置文件applicationConfig.properties
 * 4项根据dynamic的属性判断是否动态从sysConfigService动态读取
 * 
 * @author Crystal.Sea
 * 
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
