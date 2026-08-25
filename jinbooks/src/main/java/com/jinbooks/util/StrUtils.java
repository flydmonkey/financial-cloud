package com.jinbooks.util;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.StrUtil;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

public final class StrUtils {

	private static final List<String> SQL_INJECTION_KEYWORDS = Arrays.asList(
			"--", ";", "/", "\\", "#", "drop", "create", "delete", "alter",
			"truncate", "update", "insert", "and", "or");

	private StrUtils() {
	}

	public static List<String> string2List(String string, String split) {
		return StrUtil.splitTrim(string, split);
	}

	public static boolean filtersSQLInjection(String filters) {
		return StrUtil.containsAnyIgnoreCase(filters, SQL_INJECTION_KEYWORDS.toArray(new String[0]));
	}

	public static String getPinYinShort(String name) {
		try {
			char[] chars = name.toCharArray();
			StringBuilder pinyinName = new StringBuilder(chars.length);
			for (char ch : chars) {
				pinyinName.append(getPinYinName(String.valueOf(ch)).charAt(0));
			}
			return pinyinName.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String getPinYinName(String name) {
		try {
			HanyuPinyinOutputFormat pinyinFormat = new HanyuPinyinOutputFormat();
			pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
			pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
			pinyinFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
			return PinyinHelper.toHanYuPinyinString(name, pinyinFormat, "", false);
		} catch (Exception e) {
			return "";
		}
	}
}
