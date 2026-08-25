package com.jinbooks.util;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

import cn.hutool.core.codec.Base64;

/**
 * @author Crystal.Sea
 *
 */
public class AuthorizationHeaderUtils {

	/**
	 * first UpperCase
	 */
    public static final String AUTHORIZATION = "Authorization";

    public static String createBasic(String username, String password) {
        String authUserPass = username + ":" + password;
        String encodedAuthUserPass = Base64.encode(authUserPass.getBytes(StandardCharsets.UTF_8));
        return AuthorizationHeader.Credential.BASIC + encodedAuthUserPass;
    }
    
    public static String createBearer(String bearer) {
        return AuthorizationHeader.Credential.BEARER + bearer;
    }
    
    public  static AuthorizationHeader resolve(HttpServletRequest request) {
    	String authorization = resolveBearer(request);
    	return resolve(authorization);
    }

    public static AuthorizationHeader resolve(String authorization) {
        if (StringUtils.isNotBlank(authorization) && isBasic(authorization)) {
            String decodeUserPass = new String(Base64.decode(authorization.split(" ")[1]), StandardCharsets.UTF_8);
            String []userPass =decodeUserPass.split(":");
            return new AuthorizationHeader(userPass[0],userPass[1]);
        } else {
            return new AuthorizationHeader(resolveBearer(authorization));
        }
    }

    public  static String resolveBearer(HttpServletRequest request) {
    	String authorization = request.getHeader(AUTHORIZATION);
    	if(StringUtils.isNotBlank(authorization)) {
    		return resolveBearer(authorization);
    	}
    	return null;
    }
    
    public static boolean isBasic(String basic) {
        return basic.startsWith(AuthorizationHeader.Credential.BASIC);
    }
    
    static String resolveBearer(String bearer) {
        if (StringUtils.isNotBlank(bearer) && isBearer(bearer)) {
            return bearer.split(" ")[1];
        } else {
            return bearer;
        }
    }
    
    static boolean isBearer(String bearer) {
        return bearer.toLowerCase().startsWith(AuthorizationHeader.Credential.BEARER.toLowerCase());
    }
    
   

}
