package com.jinbooks.authn.provider;


import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import com.jinbooks.authn.LoginCredential;
import com.jinbooks.authn.SignedPrincipal;
import com.jinbooks.authn.jwt.service.AuthTokenService;
import com.jinbooks.authn.realm.AbstractAuthenticationRealm;
import com.jinbooks.authn.session.Session;
import com.jinbooks.authn.session.SessionManager;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.constants.ConstsLoginType;
import com.jinbooks.constants.ConstsRoles;
import com.jinbooks.constants.ConstsStatus;
import com.jinbooks.common.client.ClientResolve;
import com.jinbooks.common.client.ClientUserAgent;
import com.jinbooks.common.client.UserAgentParser;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.context.WebConstants;
import com.jinbooks.context.WebContext;
/**
 * login Authentication abstract class.登录认证提供者抽象类
 *
 * @author Crystal.Sea
 *
 */
@Slf4j
public abstract class AbstractAuthenticationProvider {

    public static final String PROVIDER_SUFFIX = "AuthenticationProvider";

    /**
     * 认证类型
     *
     */
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

    public abstract Authentication doAuthenticate(LoginCredential credential);

    @SuppressWarnings("rawtypes")
    public boolean supports(Class authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    public Authentication authenticate(LoginCredential credential){
    	log.debug("credential {}",credential);
    	return null;
    }

    /**
     * createOnlineSession 认证成功后签发token及会话
     * @param credential
     * @param userInfo
     * @return
     */
    public UsernamePasswordAuthenticationToken createOnlineTicket(LoginCredential credential,UserInfo userInfo,ClientResolve client) {
        //create session/创建新用户会话
        Session session = new Session();

        session.setStyle(credential.getStyle());

        //set session with principal。设置认证当事人
        SignedPrincipal principal = new SignedPrincipal(userInfo,session);
        //读取用户授权角色
        List<GrantedAuthority> grantedAuthoritys = authenticationRealm.grantAuthority(userInfo);
        principal.setAuthenticated(true);
        principal.setStyle(session.getStyle());
        //判断管理员角色
        for(GrantedAuthority adminAuthority : ConstsRoles.grantedAdminAuthoritys) {
            if(grantedAuthoritys.contains(adminAuthority)) {
            	principal.setRoleAdministrators(true);
                log.trace("ROLE ADMINISTRATORS Authentication .");
            }
        }
        log.debug("Granted Authority {}" , grantedAuthoritys);

        //创建认证token
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                		principal,
                        "PASSWORD",
                        grantedAuthoritys
                );

        authenticationToken.setDetails(
                new WebAuthenticationDetails(WebContext.getRequest()));

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
            throw new BadCredentialsException(WebContext.getI18nValue("login.error.password.null"));
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
            throw new BadCredentialsException("login.error.email.null");
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
            throw new BadCredentialsException(WebContext.getI18nValue("login.error.username.null"));
        }
        return true;
    }

    protected boolean statusValid(LoginCredential loginCredential , UserInfo userInfo,ClientResolve client) {
        if (null == userInfo) {
            String i18nMessage = WebContext.getI18nValue("login.error.username");
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
