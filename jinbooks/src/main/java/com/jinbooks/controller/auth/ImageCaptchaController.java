package com.jinbooks.controller.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@RequiredArgsConstructor
@Slf4j
@RestController
public class ImageCaptchaController {

	private final HutoolCaptchaService hutoolCaptchaService;

	private final AuthTokenService authTokenService;

	@GetMapping(value = { "/api/captcha" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Message<ImageCaptcha> captchaHandleRequest(
			@RequestParam(value = "captcha", required = false, defaultValue = "text") String captchaType,
			@RequestParam(value = "state", required = false, defaultValue = "state") String state) {
		try {
			state = resolveState(state);
			HutoolCaptchaService.CaptchaImage captcha = hutoolCaptchaService.createAndStore(state, captchaType);
			log.trace("captcha state {}, type {}", state, captchaType);
			return new Message<>(new ImageCaptcha(state, captcha.imageBase64()));
		} catch (Exception e) {
			log.error("captcha generate error", e);
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
