package com.financial.cloud.dto.book;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SubjectPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -2564571685545623477L;

	Integer category;

    String code;

    String name;

    String bookId;

    String standardId;
    
    Integer status;
}
