package com.financial.cloud.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import cn.hutool.core.codec.Base64;

public final class Base64ImageUtils {

	private static final String DATA_IMAGE_PNG_PREFIX = "data:image/png;base64,";

	private Base64ImageUtils() {
	}

	public static String encodePngBytes(byte[] byteImage) {
		return DATA_IMAGE_PNG_PREFIX + Base64.encode(byteImage);
	}

	public static String encodePngImage(BufferedImage bufferedImage) throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", stream);
		return encodePngBytes(stream.toByteArray());
	}
}
