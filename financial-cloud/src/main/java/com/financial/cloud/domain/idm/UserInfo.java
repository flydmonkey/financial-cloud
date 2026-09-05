package com.financial.cloud.domain.idm;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("userinfo")
public class UserInfo extends BaseEntity  implements Serializable {
    @Serial
    private static final long serialVersionUID = 6402443942083382236L;

    public static final String CLASS_TYPE = "UserInfo";

    public  static final String DEFAULT_PASSWORD_SUFFIX = "FinancialCloud@888";

    @TableField(exist = false)
    String sessionId;

    @TableId(type = IdType.ASSIGN_ID)
    String id;


    @NotEmpty(message = MessageKeys.Validation.USER_USERNAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = MessageKeys.Validation.USER_USERNAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    protected String username;

    @NotEmpty(message = MessageKeys.Validation.USER_PASSWORD_REQUIRED, groups = {AddGroup.class})
    protected String password;

    protected String decipherable;

    protected String sharedSecret;

    protected String sharedCounter;

    /**
     * "Employee", "Supplier","Dealer","Contractor",Partner,Customer "Intern",
     * "Temp", "External", and "Unknown" .
     */
    @NotEmpty(message = MessageKeys.Validation.USER_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    protected String userType;

    @NotEmpty(message = MessageKeys.Validation.USER_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    protected String userState;


    // for user name
    @NotEmpty(message = MessageKeys.Validation.HR_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = MessageKeys.Validation.HR_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    protected String displayName;

    @Size(max = 32, message = MessageKeys.Validation.USER_NICKNAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    protected String nickName;

    @NotNull(message = MessageKeys.Validation.COMMON_SORT_ORDER_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    protected Integer sortIndex;
    protected String nameZhSpell;
    protected String nameZhShortSpell;

    protected String  pictureId;

    protected String email;

    protected int emailVerified;

    protected String mobile;

    protected int mobileVerified;

    protected String passwordQuestion;

    protected String passwordAnswer;

    protected Date passwordLastSetTime;

    protected int badPasswordCount;

    protected Date badPasswordTime;

    protected Date unLockTime;

    protected int isLocked;

    protected Date lastLoginTime;

    protected String lastLoginIp;

    protected Date lastLogoffTime;

    protected int passwordSetType;

    protected Integer loginCount;

    @TableField(exist = false)
    protected String regionHistory;

    @TableField(exist = false)
    protected String passwordHistory;

    protected Integer loginFailedCount;

    protected Date loginFailedTime;

    protected String locale;

    protected String timeZone;

    protected String preferredLanguage;

    protected Integer isOnline;

    protected String ldapDn;

    @NotNull(message = MessageKeys.Validation.COMMON_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;


    String description;

	String bookId;

    @TableField(exist = false)
    java.util.List<String> roleIds;

    @TableField(exist = false)
    java.util.List<String> bookIds;

    @TableField(exist = false)
    String syncId;

    @TableField(exist = false)
    String syncName;

    @TableField(exist = false)
    String originId;

    @TableField(exist = false)
    String originId2;

    @TableField(exist = false)
    String gradingUserId;

    public UserInfo(String username) {
    	this.username = username;
    }

    public void clearSensitive() {
		this.setPassword("");
		this.setDecipherable("");
	}
}
