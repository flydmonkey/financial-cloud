package com.jinbooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * JinBooks系统启动入口
 * @author JinBooks
 *
 */
@SpringBootApplication
public class JinBooksApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(JinBooksApplication.class, args);
	}

}
