package com.financial.cloud.captcha;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImageCaptcha {
	/**
	 * jwt
	 */
	String state;
	/**
	 * 图片验证码
	 */
	String image;

	public ImageCaptcha(String state, String image) {
		super();
		this.state = state;
		this.image = image;
	}
}
