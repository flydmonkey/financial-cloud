package com.financial.cloud.authn;

import java.util.Collection;
import java.util.List;

import com.financial.cloud.authn.core.Authority;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.common.client.ClientResolve;
import com.financial.cloud.domain.idm.UserInfo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignedPrincipal {
    private static final long serialVersionUID = -110742975439268030L;

    String sessionId;

    String style;

    String bookId;

    String userId;

    String username;

    String mobile;

    String email;

    int twoFactor;

    int passwordSetType;

    String deviceId;

    UserInfo userInfo;

    ClientResolve clientResolve;

    List<Authority> grantedAuthority;

    List<Authority> grantedApps;

    boolean authenticated;

    boolean roleAdministrators;

    boolean accountNonExpired;

    boolean accountNonLocked;

    boolean credentialsNonExpired;

    boolean enabled;

    public SignedPrincipal(UserInfo user) {
        this.userInfo = user;
        this.authenticated = true;
        this.passwordSetType = user.getPasswordSetType();
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    public SignedPrincipal(UserInfo user, Session session) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.bookId = user.getBookId();
        this.email = user.getEmail();
        this.mobile = user.getMobile();
        this.passwordSetType = user.getPasswordSetType();
        this.userInfo = user;
        this.authenticated = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
        this.sessionId = session.getId();
        this.userInfo.setSessionId(session.getId());
    }

    public Collection<Authority> getAuthorities() {
        return grantedAuthority;
    }

    public void clearTwoFactor() {
        this.twoFactor = 0;
    }

    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getUsername() {
        if (this.userInfo != null) {
            return this.userInfo.getUsername();
        }
        return this.username;
    }

    public String getPassword() {
        if (this.userInfo != null) {
            return this.userInfo.getPassword();
        }
        return null;
    }
}
