package com.financial.cloud.authn.endpoint;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tools.jackson.databind.json.JsonMapper;

import com.financial.cloud.common.Message;

@Slf4j
@Controller
@RequestMapping(value = "/api/auth")
public class RefusedPoint {

	private final JsonMapper jsonMapper;

	public RefusedPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

 	@GetMapping(value={"/refusedpoint"})
	public void refusedPoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
 		log.trace("RefusedPoint /refusedpoint.");
 		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
 	    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
 	    Message<Void> body = new Message<>(Message.FORBIDDEN, "Forbidden");
		jsonMapper.writeValue(response.getOutputStream(), body);
 	}	
}
