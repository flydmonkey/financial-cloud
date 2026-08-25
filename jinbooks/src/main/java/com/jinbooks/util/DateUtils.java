package com.jinbooks.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

public class DateUtils {
	public static final String FORMAT_DATE_DEFAULT = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYYMMDD = "yyyyMMdd";

	public static final String FORMAT_DATE_YYYY_MM_DD = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYY_MM = "yyyy-MM";

	public static final String FORMAT_DATE_ISO_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static final String FORMAT_DATE_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	public static final String FORMAT_DATE_YYYY_MM_DD_HHMM = "yyyy-MM-dd HHmm";

	public static final String FORMAT_DATE_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

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

	public static Date addMinutes(Date date, int amount) {
		return add(date, Calendar.MINUTE, amount);
	}

	public static Date add(Date date, int calendarField, int amount) {
		if (date == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		return DateUtil.offset(date, toDateField(calendarField), amount);
	}

	public static Date addDate(Date date, int year, int month, int day, int hour, int minute, int second,
			int milliSecond) {
		if (date == null) {
			return null;
		}
		Date result = date;
		result = DateUtil.offset(result, DateField.YEAR, year);
		result = DateUtil.offset(result, DateField.MONTH, month);
		result = DateUtil.offset(result, DateField.DAY_OF_MONTH, day);
		result = DateUtil.offset(result, DateField.HOUR_OF_DAY, hour);
		result = DateUtil.offset(result, DateField.MINUTE, minute);
		result = DateUtil.offset(result, DateField.SECOND, second);
		result = DateUtil.offset(result, DateField.MILLISECOND, milliSecond);
		return result;
	}

	public static Date addDate(Date date, int year, int month, int day, int hour, int minute, int second) {
		return addDate(date, year, month, day, hour, minute, second, 0);
	}

	public static Date addDate(Date date, int hour, int minute, int second) {
		return addDate(date, 0, 0, 0, hour, minute, second, 0);
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

	public static String toUtc(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtc(String date) {
		return Instant.parse(date).atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtcLocal(Date date) {
		return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault()).toString();
	}

	public static String toUtcLocal(String date) {
		return ZonedDateTime.parse(date).withZoneSameInstant(ZoneOffset.systemDefault()).toString();
	}

	private static DateField toDateField(int calendarField) {
		return switch (calendarField) {
			case Calendar.YEAR -> DateField.YEAR;
			case Calendar.MONTH -> DateField.MONTH;
			case Calendar.DATE -> DateField.DAY_OF_MONTH;
			case Calendar.HOUR, Calendar.HOUR_OF_DAY -> DateField.HOUR_OF_DAY;
			case Calendar.MINUTE -> DateField.MINUTE;
			case Calendar.SECOND -> DateField.SECOND;
			case Calendar.MILLISECOND -> DateField.MILLISECOND;
			default -> throw new IllegalArgumentException("Unsupported calendar field: " + calendarField);
		};
	}
}
