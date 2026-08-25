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

package com.jinbooks.captcha;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Service
public class HutoolCaptchaService {
	private static final Logger logger = LoggerFactory.getLogger(HutoolCaptchaService.class);

	private static final int WIDTH = 100;
	private static final int HEIGHT = 50;
	private static final int CODE_LENGTH = 4;
	private static final int LINE_COUNT = 20;

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
		logger.debug("captcha: {}, expected: {}", captcha, expected);
		if (expected != null && captcha.equals(expected)) {
			captchaStore.invalidate(state);
			return true;
		}
		return false;
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
