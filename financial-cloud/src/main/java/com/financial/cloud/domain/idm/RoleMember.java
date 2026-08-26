package com.financial.cloud.domain.idm;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


@Data
@NoArgsConstructor
@TableName("role_member")
public class RoleMember implements Serializable {

	@Serial
	private static final long serialVersionUID = -8059639972590554760L;

	@TableId(type = IdType.ASSIGN_ID)
	String id;

	String roleId;

	@TableField(exist = false)
	String roleName;

	String memberId;

	@TableField(exist = false)
	String memberName;

	// USER or POST
	String type;

	String bookId;

	@TableField(exist = false)
	String instName;
	// for user
	@TableField(exist = false)
	String username;

	@TableField(exist = false)
	String displayName;

	@TableField(exist = false)
	String jobTitle;

	@TableField(exist = false)
	int gender;
	// for post
	@TableField(exist = false)
	String postCode;

	@TableField(exist = false)
	String postName;

	// department
	@TableField(exist = false)
	String departmentId;

	@TableField(exist = false)
	String department;

	@TableField(exist = false)
	String gradingUserId;

	@TableField(fill = FieldFill.INSERT)
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	protected String createdBy;

	@TableField(fill = FieldFill.INSERT)
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
	protected Date createdDate;

	/**
	 * @param groupId
	 * @param memberId
	 * @param type
	 */
	public RoleMember(String roleId, String memberId, String type, String bookId) {
		super();
		this.roleId = roleId;
		this.memberId = memberId;
		this.type = type;
		this.bookId = bookId;
	}

	public RoleMember(String roleId, String memberId, String memberName, String type,
			String createdBy, String bookId) {
		super();
		this.roleId = roleId;
		this.memberId = memberId;
		this.memberName = memberName;
		this.type = type;
		this.createdBy = createdBy;
		this.bookId = bookId;
	}

}
