package com.jinbooks.dto.journal;

import java.math.BigDecimal;

import lombok.Data;


@Data
public class JournalSummaryDto {

	String id;
	/**
	 * 账套编码
	 */
	String bookId;
	/**
	 * 年度N期
	 */
	Integer yearPeriod;
	
	/**
	 * 选择年度N期
	 */
	String yearPeriodPicker;
	
	/**
	 * 选择年度N期开始时间
	 */
	String yearPeriodStart;
	
	/**
	 * 年度
	 */
	Integer years;
	
	/**
	 * 第*期
	 */
	Integer periods;
	
	/**
	 * 账户类型：现金cash  银行deposit
	 */
	String category;

	/**
	 * 账户编码
	 */
	String accCode;
	
	/**
	 * 账户名称
	 */
	String accName;
	
	/**
	 * 币种
	 */
	String currency;
	
	/**
	 * 期初余额
	 */
	BigDecimal openingBalance;
	
	/**
	 * 期末余额
	 */
	BigDecimal closingBalance;
	
	/**
	 * 收入
	 */
	BigDecimal income;
	
	/**
	 * 支出
	 */
	BigDecimal expenditure;
	
	/**
	 * 描述
	 */
	String description;
	
}
