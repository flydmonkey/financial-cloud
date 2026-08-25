/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 





package com.jinbooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.jinbooks.bootstrap.InitializeContext;
import com.jinbooks.bootstrap.ProductEnvironment;
import com.jinbooks.context.WebContext;

/**
 * JinBooks系统启动入口
 * @author JinBooks
 *
 */
@SpringBootApplication
public class JinBooksApplication extends SpringBootServletInitializer {
	private static final Logger _logger = LoggerFactory.getLogger(JinBooksApplication.class);
	public static void main(String[] args) {
		_logger.info("Start JinBooks Application ...");
	    ProductEnvironment.listEnvVars();

		new InitializeContext(SpringApplication.run(JinBooksApplication.class, args)).init();

		_logger.info("JinBooks at {}" , java.time.LocalDateTime.now());
		_logger.info("JinBooks Port {}",WebContext.getServerPort());
		_logger.info("JinBooks started.");
	}
	
}
