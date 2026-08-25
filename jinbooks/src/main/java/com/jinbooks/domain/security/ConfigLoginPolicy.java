package com.jinbooks.domain.security;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Crystal.Sea
 *
 */
@NoArgsConstructor
@TableName(value = "jbx_config_login_policy")
@Data
public class ConfigLoginPolicy implements Serializable {
	@Serial
	private static final long serialVersionUID = 2563009899218736769L;

	@TableId(type = IdType.ASSIGN_ID)
	String id;

	@NotNull
	int sessionValidity;

	@NotNull
	int tokenValidity;

	@NotNull
	String isFirstPasswordModify;

	@NotNull
	String captcha;

	@NotNull
	String captchaMgt;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	int twoFactor;

	@NotNull
	int loginAttempts;

	@NotNull
	String isAutoLock;

	@NotNull
	int lockInterval;

	@NotNull
	int passwordAttempts;

	@NotNull
	String passwordAttemptsCaptcha;

	@NotNull
	String scanCode;

	@NotNull
	String isMobile;

	@NotNull
	String isSocial;

	String redirectUri;

	int terminals;

	public boolean isTwoFactor(){
		return this.twoFactor > 0;
	}

}
