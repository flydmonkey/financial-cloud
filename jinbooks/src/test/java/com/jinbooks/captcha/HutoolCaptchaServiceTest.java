package com.jinbooks.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cn.hutool.captcha.generator.MathGenerator;

class HutoolCaptchaServiceTest {

	private final HutoolCaptchaService service = new HutoolCaptchaService();

	@Test
	void arithmeticCaptchaAcceptsComputedAnswer() {
		String state = "test-state";
		HutoolCaptchaService.CaptchaImage image = service.createAndStore(state, "ARITHMETIC");
		String expression = image.code();
		MathGenerator generator = new MathGenerator();

		String answer = null;
		for (int i = -50; i <= 200; i++) {
			String candidate = String.valueOf(i);
			if (generator.verify(expression, candidate)) {
				answer = candidate;
				break;
			}
		}

		assertThat(answer).isNotNull();
		assertThat(service.validate(state, answer)).isTrue();
	}

	@Test
	void textCaptchaStillUsesExactMatch() {
		String state = "text-state";
		HutoolCaptchaService.CaptchaImage image = service.createAndStore(state, "TEXT");

		assertThat(service.validate(state, image.code())).isTrue();
		assertThat(service.validate(state, "0000")).isFalse();
	}

}
