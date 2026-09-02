package com.financial.cloud.util;

import com.financial.cloud.domain.voucher.Voucher;
import org.apache.commons.lang3.StringUtils;

public class VoucherUtils {

	/**
	 * 统一对外凭证字号：记-9
	 */
	public static String createWord(String head, int num) {
		return StringUtils.defaultIfBlank(head, "记") + "-" + num;
	}

	/**
	 * 兼容旧调用签名；年月不再参与字号展示。
	 */
	public static String createWord(String head, int year, int month, int num) {
		return createWord(head, num);
	}

	/**
	 * 优先按字头+号码格式化；缺字段时回退到已存 word。
	 */
	public static String displayWord(Voucher voucher) {
		if (voucher == null) {
			return null;
		}
		return displayWord(voucher.getWordHead(), voucher.getWordNum(), voucher.getWord());
	}

	public static String displayWord(String wordHead, Integer wordNum, String fallbackWord) {
		if (StringUtils.isNotBlank(wordHead) && wordNum != null) {
			return createWord(wordHead, wordNum);
		}
		return fallbackWord;
	}
}
