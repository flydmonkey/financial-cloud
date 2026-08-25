package com.jinbooks.configuration;

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
	
    @Value("${jinbooks.id.strategy:SnowFlake}") 
    String strategy;
    
    @Value("${jinbooks.id.datacenterId:0}") 
    int datacenterId;
    
    @Value("${jinbooks.id.machineId:0}") 
    int machineId;
}
