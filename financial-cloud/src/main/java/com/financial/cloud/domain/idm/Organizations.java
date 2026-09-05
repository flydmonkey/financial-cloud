package com.financial.cloud.domain.idm;

import java.io.Serial;
import java.io.Serializable;
import com.financial.cloud.constants.common.MessageKeys;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("organizations")
public class Organizations extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 5085413816404119803L;

    public static final String CLASS_TYPE = "Organization";
    public static final String ROOT_ORG_ID = "1";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotEmpty(message = MessageKeys.Validation.ORG_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = MessageKeys.Validation.ORG_CODE_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    private String orgCode;

    @NotEmpty(message = MessageKeys.Validation.ORG_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 16, message = MessageKeys.Validation.ORG_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    private String orgName;

    @NotEmpty(message = MessageKeys.Validation.ORG_FULL_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = MessageKeys.Validation.ORG_FULL_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    private String fullName;

    private String parentId;

    private String parentCode;

    private String parentName;

    /**
     * 1. entity
     * 2. virtual
     */
    @NotEmpty(message = MessageKeys.Validation.COMMON_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String type;

    private String codePath;

    private String namePath;
    //数据库关键字，解决人大金仓数据库适配修改2023-1-30-shibanglin
//    @Column(name = "organizations.level")

    private Integer level;

    private String hasChild;

    private String division;

    private String country;

    private String region;

    private String locality;

    private String street;

    private String address;

    private String contact;

    private String postalCode;

    private String phone;

    private String fax;

    private String email;

    private long sortIndex;

    private String ldapDn;

    private String description;

    private String extraAttrs;

    private int status;

    @TableField(updateStrategy = FieldStrategy.NEVER)
	private String bookId;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

    @TableField(exist = false)
	String instName;

    @TableField(exist = false)
    String syncId;

    @TableField(exist = false)
    String syncName;

    @TableField(exist = false)
    String originId;

    @TableField(exist = false)
    String originId2;

    /**
     * 1任职机构，0兼职机构
     */
    @TableField(exist = false)
    int isPrimary = 0;

    @TableField(exist = false)
    boolean reorgNamePath;

    @TableField(exist = false)
    String gradingUserId;

}
