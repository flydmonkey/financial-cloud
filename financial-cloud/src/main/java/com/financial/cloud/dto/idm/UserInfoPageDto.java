package com.financial.cloud.dto.idm;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/28 11:24
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class UserInfoPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 4849764781931650281L;

	String bookId;

    String username;

    String employeeNumber;

    String mobile;

    String email;

    String userType;

    String displayName;
}
