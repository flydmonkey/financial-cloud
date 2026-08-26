package com.financial.cloud.dto.auth;

import java.io.Serializable;

import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.validation.EditGroup;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChangePassword implements Serializable {

	/**
	 *
	 */
	static final long serialVersionUID = 655065036584798162L;
	String id;
	String userId;
	String username;
	String email;
	String mobile;
	String windowsAccount;
	String employeeNumber;
	String displayName;
	String oldPassword;

	@NotEmpty(message = "新密码不能为空", groups = {EditGroup.class})
	String password;

	@NotEmpty(message = "确认密码不能为空", groups = {EditGroup.class})
	String confirmPassword;

	String decipherable;
	String bookId;
	int passwordSetType;
	String passwordLastSetTime;

	public ChangePassword(String username,String password) {
		this.username = username;
		this.password = password;
	}

	public ChangePassword(UserInfo userInfo) {
		this.setId(userInfo.getId());
		this.setUserId(userInfo.getId());
		this.setUsername(userInfo.getUsername());
		this.setMobile(userInfo.getMobile());
		this.setEmail(userInfo.getEmail());
		this.setDecipherable(userInfo.getDecipherable());
		this.setPassword(userInfo.getPassword());
		this.setBookId(userInfo.getBookId());
	}

	public void clearPassword() {
		this.password ="";
		this.decipherable = "";
	}
}
