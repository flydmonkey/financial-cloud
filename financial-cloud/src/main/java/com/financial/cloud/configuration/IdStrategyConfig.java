package com.financial.cloud.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Configuration
public class IdStrategyConfig {
	
    @Value("${financial-cloud.id.strategy:SnowFlake}") 
    String strategy;
    
    @Value("${financial-cloud.id.datacenterId:0}") 
    int datacenterId;
    
    @Value("${financial-cloud.id.machineId:0}") 
    int machineId;
}
