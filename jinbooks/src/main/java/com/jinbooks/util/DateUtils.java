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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	public static final String FORMAT_DATE_DEFAULT = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYYMMDD = "yyyyMMdd";

	public static final String FORMAT_DATE_YYYY_MM_DD = "yyyy-MM-dd";

	public static final String FORMAT_DATE_YYYY_MM = "yyyy-MM";

	public static final String FORMAT_DATE_ISO_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static final String FORMAT_DATE_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	public static final String FORMAT_DATE_YYYY_MM_DD_HHMM = "yyyy-MM-dd HHmm";

	public static final String FORMAT_DATE_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

	public static final String getCurrentDateTimeAsString() {
		return getCurrentDateAsString(FORMAT_DATE_YYYY_MM_DD_HH_MM_SS);
	}

	public static final String getCurrentDateAsString(String formatPattern) {
		Date date = new Date();
		return format(date, formatPattern);
	}

	public static final Date getCurrentDate() {
		return new Date();
	}

	public static final String format(Date date) {
		if (date == null) {
			return "";
		}
		return format(date, FORMAT_DATE_DEFAULT);
	}

	public static final String formatDateTime(Date date) {
		if (date == null) {
			return "";
		}
		return format(date, FORMAT_DATE_YYYY_MM_DD_HH_MM_SS);
	}

	public static final String format(Date date, String formatPattern) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(formatPattern).format(date);
	}

	public static final Date parse(String stringValue, String formatPattern) {
		SimpleDateFormat format = new SimpleDateFormat(formatPattern);
		try {
			return format.parse(stringValue);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Date addMinutes(Date date, int amount) {
		return add(date, Calendar.MINUTE, amount);
	}

	public static Date add(Date date, int calendarField, int amount) {
		if (date == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(calendarField, amount);
		return c.getTime();
	}

	public static Date addDate(Date date, int year, int month, int day, int hour, int minute, int second,
			int milliSecond) {
		if (date == null) {
			return null;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		c.add(Calendar.MONTH, month);
		c.add(Calendar.DATE, day);
		c.add(Calendar.HOUR_OF_DAY, hour);
		c.add(Calendar.MINUTE, minute);
		c.add(Calendar.SECOND, second);
		c.add(Calendar.MILLISECOND, milliSecond);

		return c.getTime();
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

	public static String toUtc(java.util.Date date) {
		return Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtc(String date) {
		return Instant.parse(date).atOffset(ZoneOffset.UTC).toString();
	}

	public static String toUtcLocal(java.util.Date date) {
		return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault()).toString();
	}

	public static String toUtcLocal(String date) {
		return ZonedDateTime.parse(date).withZoneSameInstant(ZoneOffset.systemDefault()).toString();
	}

}
