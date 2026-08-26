package com.financial.cloud.dto.journal;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper=false)
public class JournalAccountPageDto extends PageQuery {

    /**
	 * 
	 */
	private static final long serialVersionUID = 438079086162069142L;
	String bookId;
	
	String accCode;
    
    String accName;
}
