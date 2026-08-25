/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

 

 

package com.jinbooks.authn;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.jinbooks.authn.session.Session;
import com.jinbooks.context.WebConstants;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ç»å½æäº¤çä¿¡æ¯å±æ?
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
public class LoginCredential  implements Authentication {
    private static final long serialVersionUID = 3125709257481600320L;
    String style =Session.STYLE.WEB;
    String congress;
    String username;
    String password;
    String captcha;
    String mobile;
    String otpCaptcha;
    String remeberMe;
    String authType;
    String secretKey;
    String deviceId;
    String state;
    String jwtToken;
    String onlineTicket;
    String provider;
    String code;
    String message = WebConstants.LOGIN_RESULT.SUCCESS;
    String instId;
    
    
    List<GrantedAuthority> grantedAuthority;
    boolean authenticated;
    boolean roleAdministrators;

    /**
     * BasicAuthentication.
     */
    public LoginCredential(String username,String password,String authType) {
        this.username = username;
        this.password = password;
        this.authType = authType;
    }
    

	@Override
    public String getName() {
        return "Login Credential";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthority;
    }

    @Override
    public Object getCredentials() {
        return this.getPassword();
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.getUsername();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;

    }
  
}
