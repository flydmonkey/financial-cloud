package com.jinbooks.dto.book;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:20
 */

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
