package com.financial.cloud.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import cn.hutool.core.date.DateUtil;

public final class DateUtils {
	public static final String FORMAT_DATE_DEFAULT = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYY_MM_DD = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYY_MM = "yyyy-MM";

	public static final String FORMAT_DATE_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	private DateUtils() {
	}

	public static String getCurrentDateTimeAsString() {
		return getCurrentDateAsString(FORMAT_DATE_YYYY_MM_DD_HH_MM_SS);
	}

	public static String getCurrentDateAsString(String formatPattern) {
		return format(new Date(), formatPattern);
	}

	public static Date getCurrentDate() {
		return new Date();
	}

	public static String format(Date date) {
		if (date == null) {
			return "";
		}
		return format(date, FORMAT_DATE_DEFAULT);
	}

	public static String formatDateTime(Date date) {
		if (date == null) {
			return "";
		}
		return format(date, FORMAT_DATE_YYYY_MM_DD_HH_MM_SS);
	}

	public static String format(Date date, String formatPattern) {
		if (date == null) {
			return "";
		}
		return DateUtil.format(date, formatPattern);
	}

	public static Date parse(String stringValue, String formatPattern) {
		try {
			return DateUtil.parse(stringValue, formatPattern);
		} catch (Exception e) {
			return null;
		}
	}

	public static LocalDate lastDay(LocalDate day) {
		int year = day.getYear();
		int month = day.getMonthValue();
		YearMonth yearMonth = YearMonth.of(year, month);
		return yearMonth.atEndOfMonth();
	}

	public static LocalDate lastDay(String dayString) {
		return lastDay(
				LocalDate.parse(
						dayString,
						DateTimeFormatter.ofPattern(FORMAT_DATE_YYYY_MM_DD)));
	}

	public static LocalDate lastDay(Date day) {
		return lastDay(format(day));
	}
}
