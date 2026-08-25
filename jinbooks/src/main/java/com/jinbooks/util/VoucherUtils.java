package com.jinbooks.util;

import cn.hutool.core.util.StrUtil;

public class VoucherUtils {
	/**
	 * 凭证字号位数
	 */
	public static final int VOUCHER_WORD_NUM_LEN = 4;

	public static String createWord(String head, int year, int month, int num) {
		return head + formatYear(year) + formatMonth(month) + "第" + formatNumber(num) + "号";
	}

	public static String formatNumber(int number) {
		return StrUtil.padPre(String.valueOf(number), VOUCHER_WORD_NUM_LEN, '0');
	}

	public static String formatYear(int year) {
		return StrUtil.padPre(String.valueOf(year), 4, '0');
	}

	public static String formatMonth(int month) {
		return StrUtil.padPre(String.valueOf(month), 2, '0');
	}
}
