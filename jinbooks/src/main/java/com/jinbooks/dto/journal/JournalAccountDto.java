package com.jinbooks.dto.journal;

import java.math.BigDecimal;

import lombok.Data;



@Data
public class JournalAccountDto {

	String id;
	
	String bookId;
	/**
	 * 账户类型：现金cash  银行deposit
	 */
	String category;
	
	String remark;

	String accCode;
	
	String accName;
	
	String subjectId;
	
	String currency;
	
	String bankNo;
	
	String bank;
	
	Integer sortIndex;
	/**
	 * 账户余额
	 */
	BigDecimal balance;
	
	String description;
}
