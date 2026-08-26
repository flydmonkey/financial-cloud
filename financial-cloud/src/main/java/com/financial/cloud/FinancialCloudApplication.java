package com.financial.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 财务云系统启动入口
 * @author financial-cloud
 *
 */
@SpringBootApplication
public class FinancialCloudApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(FinancialCloudApplication.class, args);
	}

}
