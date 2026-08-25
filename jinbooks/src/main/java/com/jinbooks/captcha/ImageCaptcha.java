package com.jinbooks.captcha;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片验证码信息
 *
 * @author Crystal.Sea
 *
 */

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
