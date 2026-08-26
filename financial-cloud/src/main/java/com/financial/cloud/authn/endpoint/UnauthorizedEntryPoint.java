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

/**
 * 未认证接口 /auth/entrypoint，以 Message 信封返回 401、提示信息和时间戳。
 * 
 * @author Crystal.Sea
 *
 */
@Slf4j
@Controller
@RequestMapping(value = "/api/auth")
public class UnauthorizedEntryPoint {

	private final JsonMapper jsonMapper;

	public UnauthorizedEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

 	@GetMapping(value={"/entrypoint"})
	public void entryPoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
 		log.trace("UnauthorizedEntryPoint /entrypoint.");
 		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
 	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 	    Message<Void> body = new Message<>(Message.UNAUTHORIZED, "Unauthorized");
		jsonMapper.writeValue(response.getOutputStream(), body);
 	}	
 	
}
