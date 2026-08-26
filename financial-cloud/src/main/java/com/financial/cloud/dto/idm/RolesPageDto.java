package com.financial.cloud.dto.idm;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/25 9:29
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class RolesPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 7889062435661661753L;

	private String bookId;

    private String roleName;
}
