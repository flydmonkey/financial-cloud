package com.financial.cloud.dto.journal;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;



@Data
public class JournalEntryDto {

	String id;
	
	String bookId;
	/**
	 * 账户类型：现金cash  银行deposit
	 */
	String category;
	
	String remark;
	
	String accId;
	
	String accCode;
	
	String accName;
	
	String subjectId;
	
	String voucherId;
	/**
	 * 方向收入和支出，  I或E，Income and Expenditure
	 */
	String direction;
	
	BigDecimal income;
	
	BigDecimal expenditure;
	
	BigDecimal balance;
	
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
	Date tradeDate;
	
	String description;
}
