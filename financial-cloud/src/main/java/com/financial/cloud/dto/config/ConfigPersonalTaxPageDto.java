package com.financial.cloud.dto.config;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
