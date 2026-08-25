package com.jinbooks.dto.book;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:20
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class BookPageDto extends PageQuery {

    /**
	 * 
	 */
	private static final long serialVersionUID = -496174929695190023L;
	String name;
}
