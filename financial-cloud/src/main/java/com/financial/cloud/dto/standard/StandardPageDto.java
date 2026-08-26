package com.financial.cloud.dto.standard;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/27 16:46
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class StandardPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 548118593262511361L;
	String name;
}
