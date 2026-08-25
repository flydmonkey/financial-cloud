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


package com.jinbooks.util;

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
