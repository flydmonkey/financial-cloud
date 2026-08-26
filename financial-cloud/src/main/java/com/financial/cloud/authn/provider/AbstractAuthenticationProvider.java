package com.financial.cloud.authn.provider;


import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.authn.core.AuthDetails;
import com.financial.cloud.authn.core.Authority;
import com.financial.cloud.authn.core.BadCredentialsException;

import com.financial.cloud.authn.LoginCredential;
import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.realm.AbstractAuthenticationRealm;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.constants.auth.ConstsLoginType;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.constants.auth.ConstsRoles;
import com.financial.cloud.constants.common.ConstsStatus;
import com.financial.cloud.common.client.ClientResolve;
import com.financial.cloud.common.client.ClientUserAgent;
import com.financial.cloud.common.client.UserAgentParser;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;
@Slf4j
public abstract class AbstractAuthenticationProvider {

    public static final String PROVIDER_SUFFIX = "AuthenticationProvider";

    public class AuthType{
    	//用户名和密码登录
    	public static final  String NORMAL 		= "normal";
    	//双因素认证
    	public static final  String TFA 		= "tfa";
    	//手机号码登录
    	public static final  String MOBILE 		= "mobile";
    }

    //认证域
    protected AbstractAuthenticationRealm authenticationRealm;
    //认证令牌服务
    protected AuthTokenService authTokenService;
    //会话管理
    protected SessionManager sessionManager;
    //登录校验服务
    protected LoginService loginService;

    protected boolean supported = true;

    public abstract String getProviderName();

    public abstract AuthAuthentication doAuthenticate(LoginCredential credential);

    @SuppressWarnings("rawtypes")
    public boolean supports(Class authentication) {
        return LoginCredential.class.isAssignableFrom(authentication);
    }

    public AuthAuthentication authenticate(LoginCredential credential) {
    	log.debug("credential {}",credential);
    	return null;
    }

    /**
     * createOnlineSession 认证成功后签发token及会话
     * @param credential
     * @param userInfo
     * @return
     */
    public AuthAuthentication createOnlineTicket(LoginCredential credential, UserInfo userInfo, ClientResolve client) {
        //create session/创建新用户会话
        Session session = new Session();

        session.setStyle(credential.getStyle());

        //set session with principal。设置认证当事人
        SignedPrincipal principal = new SignedPrincipal(userInfo,session);
        //读取用户授权角色
        List<Authority> grantedAuthoritys = authenticationRealm.grantAuthority(userInfo);
        principal.setAuthenticated(true);
        principal.setStyle(session.getStyle());
        //判断管理员角色
        for (Authority adminAuthority : ConstsRoles.grantedAdminAuthoritys) {
            if (grantedAuthoritys.contains(adminAuthority)) {
            	principal.setRoleAdministrators(true);
                log.trace("ROLE ADMINISTRATORS Authentication .");
            }
        }
        log.debug("Granted Authority {}" , grantedAuthoritys);

        AuthAuthentication authenticationToken =
                AuthAuthentication.authenticated(principal, "PASSWORD", grantedAuthoritys);

        authenticationToken.setDetails(new AuthDetails(WebContext.getRequest()));

        /*
         *  put Authentication to current session context，设置会话的认证token
         */
        session.setAuthentication(authenticationToken);
        sessionManager.create(session.getId(), session);

        //set Authentication to http session，设置当前认证token
        AuthorizationUtils.setAuthentication(authenticationToken);

        return authenticationToken;
    }

    /**
     * login user by username ， userinfo is null,query user from system.
     *
     * @param username String
     * @param password String
     * @return
     */
    public UserInfo loadUserInfo(String username, String password) {
    	return authenticationRealm.loadUserInfo(username, password);
    }

    /**
     * check input password empty.
     *
     * @param password String
     * @return
     */
    protected boolean emptyPasswordValid(String password) {
        if (null == password || "".equals(password)) {
            throw new BadCredentialsException(WebContext.getI18nValue(MessageKeys.Login.ERROR_PASSWORD_NULL));
        }
        return true;
    }

    /**
     * check input username or password empty.
     *
     * @param email String
     * @return
     */
    protected boolean emptyEmailValid(String email) {
        if (null == email || "".equals(email)) {
            throw new BadCredentialsException(WebContext.getI18nValue(MessageKeys.Login.ERROR_EMAIL_NULL));
        }
        return true;
    }

    /**
     * check input username empty.
     *
     * @param username String
     * @return
     */
    protected boolean emptyUsernameValid(String username) {
        if (null == username || "".equals(username)) {
            throw new BadCredentialsException(WebContext.getI18nValue(MessageKeys.Login.ERROR_USERNAME_NULL));
        }
        return true;
    }

    protected boolean statusValid(LoginCredential loginCredential , UserInfo userInfo,ClientResolve client) {
        if (null == userInfo) {
            String i18nMessage = WebContext.getI18nValue(MessageKeys.Login.ERROR_USERNAME);
            log.debug("login user {} not in this System {}." ,loginCredential.getUsername(), i18nMessage);
            UserInfo loginUser = new UserInfo(loginCredential.getUsername());
            loginUser.setId(WebContext.genId());
            loginUser.setUsername(loginCredential.getUsername());
            loginUser.setDisplayName("not exist");
            loginUser.setLoginCount(0);
            authenticationRealm.insertLoginHistory(
            			loginUser,
            			client,
            			ConstsLoginType.NORMAL,
            			"",
            			i18nMessage,
            			WebConstants.LOGIN_RESULT.USER_NOT_EXIST);
            throw new BadCredentialsException(i18nMessage);
        }else {
        	if(userInfo.getIsLocked()==ConstsStatus.LOCK) {
        		authenticationRealm.insertLoginHistory(
        				userInfo,
        				client,
                        loginCredential.getAuthType(),
                        loginCredential.getProvider(),
                        loginCredential.getCode(),
                        WebConstants.LOGIN_RESULT.USER_LOCKED
                    );
        	}else if(userInfo.getStatus()!=ConstsStatus.ACTIVE) {
        		authenticationRealm.insertLoginHistory(
        				userInfo,
        				client,
                        loginCredential.getAuthType(),
                        loginCredential.getProvider(),
                        loginCredential.getCode(),
                        WebConstants.LOGIN_RESULT.USER_INACTIVE
                    );
        	}
        }
        return true;
    }

    public ClientResolve parserClientResolve() {
    	ClientUserAgent clientUserAgent = UserAgentParser.resolveUserAgent(WebContext.getRequest());
        ClientResolve clientResolve = new ClientResolve(clientUserAgent);
        clientResolve.setIpAddr(WebContext.getRequestIpAddress());
    	return clientResolve;
    }

	public boolean isSupported() {
		return supported;
	}

	public void setSupported(boolean supported) {
		this.supported = supported;
	}

}
