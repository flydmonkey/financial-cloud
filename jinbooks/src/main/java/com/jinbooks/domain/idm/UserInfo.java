package com.jinbooks.domain.idm;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseEntity;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * .
 * @author Crystal.Sea
 *
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("userinfo")
public class UserInfo extends BaseEntity  implements Serializable {
    @Serial
    private static final long serialVersionUID = 6402443942083382236L;

    public static final String CLASS_TYPE = "UserInfo";

    public  static final String DEFAULT_PASSWORD_SUFFIX = "JinBooks@888";

    @TableField(exist = false)
    String sessionId;

    @TableId(type = IdType.ASSIGN_ID)
    String id;


    @NotEmpty(message = "用户名不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = "用户名长度不能超过32位", groups = {AddGroup.class, EditGroup.class})
    protected String username;

    @NotEmpty(message = "密码不能为空", groups = {AddGroup.class})
    protected String password;

    protected String decipherable;

    protected String sharedSecret;

    protected String sharedCounter;

    /**
     * "Employee", "Supplier","Dealer","Contractor",Partner,Customer "Intern",
     * "Temp", "External", and "Unknown" .
     */
    @NotEmpty(message = "用户类型不能为空", groups = {AddGroup.class, EditGroup.class})
    protected String userType;

    @NotEmpty(message = "用户状态不能为空", groups = {AddGroup.class, EditGroup.class})
    protected String userState;


    // for user name
    @NotEmpty(message = "姓名不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 32, message = "姓名的长度不能超过32位", groups = {AddGroup.class, EditGroup.class})
    protected String displayName;

    @Size(max = 32, message = "昵称的长度不能超过32位", groups = {AddGroup.class, EditGroup.class})
    protected String nickName;

    @NotNull(message = "排序序号不能为空", groups = {AddGroup.class, EditGroup.class})
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

    @NotNull(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    Integer status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;


    String description;

	String bookId;

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
