package com.financial.cloud.dto.permissions;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
