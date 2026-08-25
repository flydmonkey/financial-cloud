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
 

 

 

package com.jinbooks.authn.endpoint;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tools.jackson.databind.json.JsonMapper;

import com.jinbooks.common.Message;

/**
 * 未认证接口 /auth/entrypoint，以 Message 信封返回 401、提示信息和时间戳。
 * 
 * @author Crystal.Sea
 *
 */
@Controller
@RequestMapping(value = "/auth")
public class UnauthorizedEntryPoint {
	private static final Logger logger = LoggerFactory.getLogger(UnauthorizedEntryPoint.class);

	private final JsonMapper jsonMapper;

	public UnauthorizedEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

 	@GetMapping(value={"/entrypoint"})
	public void entryPoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
 		logger.trace("UnauthorizedEntryPoint /entrypoint.");
 		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
 	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 	    Message<Void> body = new Message<>(Message.UNAUTHORIZED, "Unauthorized");
		jsonMapper.writeValue(response.getOutputStream(), body);
 	}	
 	
}
