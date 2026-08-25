package com.jinbooks.controller.auth;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import org.apache.commons.lang.SystemUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.jinbooks.context.WebContext;

/**
 * ProductVersion
 * @author Crystal.Sea
 *
 */

@Slf4j
@Controller
public class ProductVersionController {
	
	static final String VERSION_STRING ="""
			<!DOCTYPE html>
			<html>
			<head>
			    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
			    <link rel="shortcut icon" type="image/x-icon" href="%s/static/favicon.ico"/>
			    <base href='%s'/> 
			    <title>JinBooks Accounting Software</title>
			</head>
			<body>
			    <center>
			        <hr>
			        JinBooks Community Edition <br>
			        Accounting Software <br>
			        Version %s <br>
			        <br>
			        <hr>
			        JAVA &nbsp&nbsp : &nbsp&nbsp %s java version %s, class %s<br>
			                %s (build %s, %s)<br>
			        <hr>
			    </center>
			</body>
			</html>
			""";

	@GetMapping(value={"/api/"})
	public void version(HttpServletRequest request,HttpServletResponse response) throws IOException {
		log.debug("ProductVersion /");
		ServletOutputStream out = response.getOutputStream();
		String contextPath = request.getContextPath();
		out.println(
				String.format(
						VERSION_STRING,
						contextPath,
						contextPath,
						WebContext.getProperty("application.formatted-version"),
						SystemUtils.JAVA_VENDOR,
                        SystemUtils.JAVA_VERSION,
                        SystemUtils.JAVA_CLASS_VERSION,
                        SystemUtils.JAVA_VM_NAME,
                        SystemUtils.JAVA_VM_VERSION,
                        SystemUtils.JAVA_VM_INFO));
		out.close();
	}
	
}
