package com.jinbooks.captcha;


import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.jinbooks.constants.ConstsCaptchaType;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Hutool 图形验证码：生成 + Caffeine 内存校验。
 */
@Slf4j
@Service
public class HutoolCaptchaService {

	private static final int WIDTH = 100;
	private static final int HEIGHT = 50;
	private static final int CODE_LENGTH = 4;
	private static final int LINE_COUNT = 20;

	private static final MathGenerator MATH_GENERATOR = new MathGenerator();

	private final Cache<String, String> captchaStore = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES)
			.maximumSize(200_000)
			.build();

	public CaptchaImage createAndStore(String state, String captchaType) {
		CaptchaImage captcha = create(captchaType);
		captchaStore.put(state, captcha.code());
		return captcha;
	}

	public boolean validate(String state, String captcha) {
		if (StringUtils.isBlank(state) || StringUtils.isBlank(captcha)) {
			return false;
		}
		String expected = captchaStore.getIfPresent(state);
		log.debug("captcha: {}, expected: {}", captcha, expected);
		if (expected != null && matchesCaptcha(expected, captcha)) {
			captchaStore.invalidate(state);
			return true;
		}
		return false;
	}

	private boolean matchesCaptcha(String expected, String userInput) {
		if (isArithmeticExpression(expected)) {
			return MATH_GENERATOR.verify(expected, userInput);
		}
		return userInput.equals(expected);
	}

	private boolean isArithmeticExpression(String code) {
		return code.indexOf('=') >= 0 || code.indexOf('+') >= 0 || code.indexOf('-') >= 0
				|| code.indexOf('*') >= 0 || code.indexOf('/') >= 0 || code.indexOf('x') >= 0;
	}

	public CaptchaImage create(String captchaType) {
		if (isArithmetic(captchaType)) {
			ShearCaptcha captcha = new ShearCaptcha(WIDTH, HEIGHT, CODE_LENGTH, 4);
			captcha.setGenerator(new MathGenerator());
			captcha.createCode();
			return new CaptchaImage(captcha.getCode(), captcha.getImageBase64Data());
		}
		LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_LENGTH, LINE_COUNT);
		captcha.setGenerator(new RandomGenerator("0123456789", CODE_LENGTH));
		captcha.createCode();
		return new CaptchaImage(captcha.getCode(), captcha.getImageBase64Data());
	}

	private boolean isArithmetic(String captchaType) {
		return ConstsCaptchaType.ARITHMETIC.equalsIgnoreCase(captchaType)
				|| "Arithmetic".equalsIgnoreCase(captchaType);
	}

	public record CaptchaImage(String code, String imageBase64) {
	}
}
