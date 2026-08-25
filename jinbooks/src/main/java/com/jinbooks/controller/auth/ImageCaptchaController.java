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

package com.jinbooks.controller.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.jwt.service.AuthTokenService;
import com.jinbooks.captcha.HutoolCaptchaService;
import com.jinbooks.captcha.ImageCaptcha;
import com.jinbooks.common.Message;

/**
 * 图片验证码，使用 Hutool 生成。
 */
@RestController
public class ImageCaptchaController {
	private static final Logger logger = LoggerFactory.getLogger(ImageCaptchaController.class);

	@Autowired
	HutoolCaptchaService hutoolCaptchaService;

	@Autowired
	AuthTokenService authTokenService;

	@GetMapping(value = { "/captcha" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Message<ImageCaptcha> captchaHandleRequest(
			@RequestParam(value = "captcha", required = false, defaultValue = "text") String captchaType,
			@RequestParam(value = "state", required = false, defaultValue = "state") String state) {
		try {
			state = resolveState(state);
			HutoolCaptchaService.CaptchaImage captcha = hutoolCaptchaService.createAndStore(state, captchaType);
			logger.trace("captcha state {}, type {}", state, captchaType);
			return new Message<>(new ImageCaptcha(state, captcha.imageBase64()));
		} catch (Exception e) {
			logger.error("captcha generate error", e);
		}
		return new Message<>(Message.FAIL);
	}

	private String resolveState(String state) {
		if (StringUtils.isBlank(state) || state.equalsIgnoreCase("state")) {
			return authTokenService.genState();
		}
		return state;
	}
}
