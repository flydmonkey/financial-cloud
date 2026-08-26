package com.financial.cloud.dto.idm;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class OrgPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 4488795848556645003L;

	private String bookId;

    private String orgName;

    private String parentId;

    private String parentName;

    private String id;
}
