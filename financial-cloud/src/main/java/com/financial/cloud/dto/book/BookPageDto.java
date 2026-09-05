package com.financial.cloud.dto.book;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class BookPageDto extends PageQuery {

    /**
	 * 
	 */
	private static final long serialVersionUID = -496174929695190023L;
	String name;
	/** Current user id — list only books granted via permission_book. */
	String userId;
}
