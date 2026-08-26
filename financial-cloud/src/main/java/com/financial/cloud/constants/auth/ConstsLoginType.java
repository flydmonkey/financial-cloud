package com.financial.cloud.constants.auth;

public class ConstsLoginType {

	/**
	 * 本地登录
	 */
    public static final String NORMAL 			= "normal";
    
    /**
	 * 手机验证码登录
	 */
	public static final String MOBILE 			= "Mobile";

    public static final String JWT 				= "Jwt";
    
    public static final String CAS 				= "CAS";

    /**
     * 第三方登录
     */
    public static final String SOCIALSIGNON 	= "Social Sign On";
    
    public static final class TwoFactor{
    	/**
    	 * 2=邮箱验证码
    	 */
    	public static final String TWO_FACTOR_EMAIL 	= "TwoFactorEmail";
    	/**
    	 * 3=手机短信
    	 */
    	public static final String TWO_FACTOR_MOBILE 	= "TwoFactorMobile";
    }
}
