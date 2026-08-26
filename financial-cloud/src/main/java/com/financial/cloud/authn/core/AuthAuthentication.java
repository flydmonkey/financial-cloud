package com.financial.cloud.authn.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Data;

@Data
public class AuthAuthentication {

    private Object principal;

    private Object credentials;

    private List<Authority> authorities = new ArrayList<>();

    private boolean authenticated;

    private Object details;

    public AuthAuthentication() {
    }

    public AuthAuthentication(Object principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
    }

    public static AuthAuthentication authenticated(Object principal, Object credentials, List<Authority> authorities) {
        AuthAuthentication authentication = new AuthAuthentication(principal, credentials);
        authentication.setAuthorities(authorities);
        authentication.setAuthenticated(true);
        return authentication;
    }

    public Collection<Authority> getAuthorities() {
        return authorities;
    }
}
