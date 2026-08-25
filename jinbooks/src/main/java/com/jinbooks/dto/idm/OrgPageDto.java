package com.jinbooks.dto.idm;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/25 9:59
 */

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
