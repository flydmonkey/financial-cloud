package com.financial.cloud.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Configuration
public class ApplicationConfig {

    @Value("${server.port:8080}")
    private int port;

    @Value("#{'${financial-cloud.server.restrict.hosts:}'.split(',')}")
	List<String> restrictHosts;
    
    @Value("${financial-cloud.job.cron.schedule:0 0 0/1 * * ?}") 
    String jobCronSchedule;

}
