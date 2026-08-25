package com.jinbooks.dto.config;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/6 17:55
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class ConfigPersonalTaxPageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = 755863428943909304L;

	String bookId;

    Integer type;
}
