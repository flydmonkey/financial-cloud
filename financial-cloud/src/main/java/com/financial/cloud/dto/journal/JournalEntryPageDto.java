package com.financial.cloud.dto.journal;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper=false)
public class JournalEntryPageDto extends PageQuery {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4408936492128287030L;
	String bookId;
	
	String accCode;
    
    String accName;
    
    String remark;
}
