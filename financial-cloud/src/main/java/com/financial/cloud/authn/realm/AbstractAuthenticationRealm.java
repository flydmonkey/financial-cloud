package com.financial.cloud.authn.realm;


import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.List;

import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.authn.core.Authority;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.common.client.ClientResolve;
import com.financial.cloud.domain.history.HistoryLogin;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

/**
 * AbstractAuthenticationRealm.认证域抽象类
 *
 * @author Crystal.Sea
 *
 */
@Slf4j
public abstract class AbstractAuthenticationRealm {

    protected PasswordPolicyValidatorService passwordPolicyValidator;

    protected LoginService loginService;

    /**
     *
     */
    public AbstractAuthenticationRealm() {

    }

    public PasswordPolicyValidatorService getPasswordPolicyValidator() {
        return passwordPolicyValidator;
    }



    public UserInfo loadUserInfo(String username, String password) {
        return loginService.findByUsername(username);
    }

    public abstract boolean passwordMatches(UserInfo userInfo, String password);

    /**
     * grant Authority by userinfo
     *
     * @param userInfo
     * @return ArrayList<GrantedAuthority>
     */
    public List<Authority> grantAuthority(UserInfo userInfo) {
        return loginService.grantAuthority(userInfo);
    }

    public void applyLoginPolicy(UserInfo userInfo) {
    	this.loginService.applyLoginPolicy(userInfo);
    }

    /**
     * login log write to log db
     *
     * @param userInfo
     * @param client
     * @param type
     * @param code
     * @param message
     */
    public boolean insertLoginHistory(UserInfo userInfo,ClientResolve client, String type, String provider, String code, String message) {
        HistoryLogin historyLogin = new HistoryLogin();
        historyLogin.setSessionId(WebContext.genId());
        AuthAuthentication authentication = (AuthAuthentication) WebContext.getAttribute(WebConstants.AUTHENTICATION);
        if(authentication != null
        		&& authentication.getPrincipal() instanceof SignedPrincipal principal) {
              historyLogin.setSessionId(principal.getSessionId());
              historyLogin.setStyle(principal.getStyle());
        }

        log.debug("user session id is {} . ",historyLogin.getSessionId());
        String requestIpAddress = WebContext.getRequestIpAddress();

        userInfo.setLastLoginTime(new Date());
        userInfo.setLastLoginIp(requestIpAddress);

        historyLogin.setIpAddr(userInfo.getLastLoginIp());
        historyLogin.setProvider(provider);
        historyLogin.setCode(code);
        historyLogin.setLoginType(type);
        historyLogin.setMessage(message);
        historyLogin.setUserId(userInfo.getId());
        historyLogin.setUsername(userInfo.getUsername());
        historyLogin.setDisplayName(userInfo.getDisplayName());
        historyLogin.setBookId(userInfo.getBookId());

        historyLogin.setBrowser(client.getBrowser());
        historyLogin.setPlatform(client.getPlatform());

        historyLogin.setCountry(client.getCountry());
        historyLogin.setProvince(client.getProvince());
        historyLogin.setCity(client.getCity());
        historyLogin.setLocation(client.getLocation());

        historyLogin.setOperateTime(new Date());
        //insert
        loginService.insertHistory(historyLogin);

        //update user last info
        loginService.updateLastLogin(userInfo);

        return true;
    }
}
