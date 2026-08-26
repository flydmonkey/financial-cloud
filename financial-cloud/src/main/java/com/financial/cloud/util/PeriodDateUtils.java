package com.financial.cloud.util;

import java.util.Date;

import com.financial.cloud.common.PeriodStr;

import cn.hutool.core.date.DateUtil;

public class PeriodDateUtils {

	private PeriodDateUtils() {
	}

	public static PeriodStr convertToPeriod(Date date) {
		return new PeriodStr(
				DateUtil.formatDate(DateUtil.beginOfMonth(date)),
				DateUtil.formatDate(DateUtil.endOfMonth(date)));
	}
}
