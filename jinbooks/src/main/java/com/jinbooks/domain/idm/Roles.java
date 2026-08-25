package com.jinbooks.domain.idm;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.constants.ConstsRoles;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author 24096
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName( "JBX_ROLES")
public class Roles extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 4660258495864814777L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    @NotEmpty(message = "用户组编码不能为空", groups = {AddGroup.class, EditGroup.class})
    String roleCode;

    @NotEmpty(message = "用户组名称不能为空", groups = {AddGroup.class, EditGroup.class})
    String roleName;

    String pattern;

    String category;

    String filters ;

    String orgIdsList;

    int isdefault;

    String description;

    int status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

    @TableField(exist = false)
	private String instName;

    @TableField(exist = false)
	String gradingUserId;


    public Roles(String id) {
        this.id = id;
    }

    /**
     * Groups.
     * @param id String
     * @param groupName String
     * @param isdefault int
     */
    public Roles(String id, String roleName, int isdefault) {
        super();
        this.id = id;
        this.roleName = roleName;
        this.isdefault = isdefault;
    }

    public Roles(String id, String roleCode,String roleName, int isdefault) {
        super();
        this.id = id;
        this.roleCode = roleName;
        this.roleName = roleName;
        this.isdefault = isdefault;
    }

	/**
     * ROLE_ALL_USER must be
     * 		1, dynamic
     * 		2, all orgIdsList
	 *		3, not filters
     */
    public void setDefaultAllUser() {
    	this.pattern = ConstsRoles.Pattern.DYNAMIC;
    	this.orgIdsList ="";
		this.filters ="";
    }
}
