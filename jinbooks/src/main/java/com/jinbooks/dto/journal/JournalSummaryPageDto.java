package com.jinbooks.dto.journal;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper=false)
public class JournalSummaryPageDto extends PageQuery {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2209525588972406022L;

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
	 * 年度
	 */
	Integer years;
	
	/**
	 * 第*期
	 */
	Integer periods;
	
	/**
	 * 账户编码
	 */
    String accCode;
    
    /**
     * 账户名称
     */
    String accName;

}
