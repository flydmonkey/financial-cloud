package com.financial.cloud.dto.book;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SettlementPageDto extends PageQuery {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3712467779731473469L;
	
	String bookId;
	
	int year;
}
