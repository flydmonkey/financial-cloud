package com.financial.cloud.authn.session;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.financial.cloud.authn.core.AuthAuthentication;
import com.financial.cloud.context.WebContext;

import lombok.Data;

@Data
public class Session implements Serializable {
    private static final long serialVersionUID = 1568480892398646468L;

    public static final int MAX_EXPIRY_DURATION = 60 * 5; //default 5 minutes.

    public static final class STYLE {
        public static final String WEB = "web";
        public static final String MGMT = "mgmt";
        public static final String APP = "app";
        public static final String PLAT = "plat";
    }

    /**
     * 会话id
     */
    String id;

    String style = Session.STYLE.WEB;

    LocalDateTime startTimestamp;

    LocalDateTime lastAccessTime;

    LocalDateTime expiredTime;
    /**
     * 认证信息
     */
    AuthAuthentication authentication;

    public Session() {
        super();
        this.id = WebContext.genId();
        this.startTimestamp = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
    }

    public Session(String sessionId) {
        super();
        this.id = sessionId;
        this.startTimestamp = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
    }

    public Session(String sessionId, AuthAuthentication authentication) {
        super();
        this.id = sessionId;
        this.authentication = authentication;
        this.startTimestamp = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
    }

}
