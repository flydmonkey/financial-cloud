package com.jinbooks.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.constants.ContentType;
import com.jinbooks.context.WebContext;

@RestController
public class MetadataController {

	@GetMapping(value = "/api/metadata/version",produces = ContentType.TEXT_PLAIN_UTF8)
	public String  metadata(HttpServletRequest request,HttpServletResponse response) {
		return WebContext.version();
	}
}
