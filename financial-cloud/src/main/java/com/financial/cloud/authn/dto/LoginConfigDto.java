package com.financial.cloud.authn.dto;

import com.financial.cloud.authn.jwt.AuthJwt;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.domain.security.SocialsProviderLogin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginConfigDto{
	/**
	 * 机构信息
	 */
	Institutions inst;
	/**
	 * 验证码
	 */
	String captcha;
	/**
	 * 状态码
	 */
	String state;
	/**
	 * 社交账号登录
	 */
	boolean isSocial;
	/**
	 * 是否首次登录修改密码
	 */
	String isFirstPasswordModify;
	/**
	 * 社交登录提供者
	 */
	SocialsProviderLogin  socials;

	/**
	 * jwt令牌
	 */
	AuthJwt authJwt;

	/**
	 * 默认跳转地址
	 */
	String redirectUri;

}
