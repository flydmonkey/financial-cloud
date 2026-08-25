package com.jinbooks.dto.journal;

import com.jinbooks.common.PageQuery;
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
