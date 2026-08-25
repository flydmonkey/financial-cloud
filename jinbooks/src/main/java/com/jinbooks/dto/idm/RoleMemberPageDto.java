package com.jinbooks.dto.idm;

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
public class RoleMemberPageDto extends PageQuery {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -6954485176508112100L;

	private String bookId;

    private String username;

    private String memberId;

    private String gradingUserId;

    private String displayName;

    private String memberName;

    private String roleId;

    private String roleName;
}
