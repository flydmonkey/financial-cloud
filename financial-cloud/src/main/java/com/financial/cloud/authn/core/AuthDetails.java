package com.financial.cloud.authn.core;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;

@Getter
public class AuthDetails {

    private final String remoteAddress;

    private final String sessionId;

    public AuthDetails(HttpServletRequest request) {
        this.remoteAddress = request.getRemoteAddr();
        this.sessionId = request.getSession(false) == null ? null : request.getSession().getId();
    }
}
