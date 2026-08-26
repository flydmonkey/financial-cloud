package com.financial.cloud.authn;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.financial.cloud.authn.core.Authority;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.context.WebConstants;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录提交的信息属性
 */
@Data
@NoArgsConstructor
public class LoginCredential {
    private static final long serialVersionUID = 3125709257481600320L;
    String style = Session.STYLE.WEB;
    @JsonAlias("congress")
    String sessionToken;
    String username;
    String password;
    String captcha;
    String mobile;
    String otpCaptcha;
    String remeberMe;
    String authType;
    String deviceId;
    String state;
    String jwtToken;
    String onlineTicket;
    String provider;
    String code;
    String message = WebConstants.LOGIN_RESULT.SUCCESS;
    String instId;

    List<Authority> grantedAuthority;
    boolean authenticated;
    boolean roleAdministrators;

    public LoginCredential(String username, String password, String authType) {
        this.username = username;
        this.password = password;
        this.authType = authType;
    }

    public Object getPrincipal() {
        return this.username;
    }
}
