package com.jinbooks.dto.permissions;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/27 15:15
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class ResourcesPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -5804284703086424324L;

	String id;

    String appId;

    String resName;

    String parentId;

    String bookId;
}
