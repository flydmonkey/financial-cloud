package com.financial.cloud.dto.permissions;

import com.financial.cloud.common.PageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionBookPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -372289040589078714L;

	private String bookId;
    
    private String bookName;

    private String userId;
}
