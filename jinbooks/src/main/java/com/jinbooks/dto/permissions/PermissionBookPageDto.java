package com.jinbooks.dto.permissions;

import com.jinbooks.common.PageQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/14 16:13
 */

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
