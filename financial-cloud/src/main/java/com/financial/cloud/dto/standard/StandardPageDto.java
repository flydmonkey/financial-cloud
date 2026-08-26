package com.financial.cloud.dto.standard;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class StandardPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 548118593262511361L;
	String name;
}
