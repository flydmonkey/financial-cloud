package com.financial.cloud.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cn.hutool.captcha.generator.MathGenerator;

class HutoolCaptchaServiceTest {

	private final HutoolCaptchaService service = new HutoolCaptchaService();

	@Test
	void arithmeticCaptchaAcceptsComputedAnswer() {
		MathGenerator generator = new MathGenerator();
		for (int attempt = 0; attempt < 20; attempt++) {
			String state = "test-state-" + attempt;
			HutoolCaptchaService.CaptchaImage image = service.createAndStore(state, "ARITHMETIC");
			String expression = image.code();
			for (int i = -200; i <= 500; i++) {
				String candidate = String.valueOf(i);
				if (generator.verify(expression, candidate)) {
					assertThat(service.validate(state, candidate)).isTrue();
					return;
				}
			}
		}
		throw new AssertionError("unable to resolve arithmetic captcha answer in test samples");
	}

	@Test
	void textCaptchaStillUsesExactMatch() {
		String state = "text-state";
		HutoolCaptchaService.CaptchaImage image = service.createAndStore(state, "TEXT");

		assertThat(service.validate(state, image.code())).isTrue();
		assertThat(service.validate(state, "0000")).isFalse();
	}

}
