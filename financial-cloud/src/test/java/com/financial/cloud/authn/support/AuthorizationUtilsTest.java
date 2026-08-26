package com.financial.cloud.authn.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.impl.InMemorySessionManager;
import com.financial.cloud.domain.idm.UserInfo;

class AuthorizationUtilsTest {

    @Test
    void authenticatesValidSession() {
        String sessionId = "session-1";
        InMemorySessionManager sessionManager = new InMemorySessionManager(3600, 100);
        Session session = buildSession(sessionId);
        sessionManager.create(sessionId, session);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + sessionId);

        AuthorizationUtils.doSessionAuthenticate(
                request,
                AuthorizationUtils.BearerType.AUTHORIZATION_TYPE,
                sessionId,
                sessionManager);

        Authentication authentication = AuthorizationUtils.getAuthentication(request);
        assertThat(authentication).isNotNull();
        SignedPrincipal principal = (SignedPrincipal) authentication.getPrincipal();
        assertThat(principal.getSessionId()).isEqualTo(sessionId);
        assertThat(principal.getUserId()).isEqualTo("user-1");
        assertThat(principal.getUsername()).isEqualTo("alice");
    }

    @Test
    void rejectsMissingSession() {
        InMemorySessionManager sessionManager = new InMemorySessionManager(3600, 100);
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthorizationUtils.doSessionAuthenticate(
                request,
                AuthorizationUtils.BearerType.AUTHORIZATION_TYPE,
                "missing-session",
                sessionManager);

        assertThat(AuthorizationUtils.getAuthentication(request)).isNull();
    }

    @Test
    void rejectsBlankSessionId() {
        InMemorySessionManager sessionManager = new InMemorySessionManager(3600, 100);
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthorizationUtils.doSessionAuthenticate(
                request,
                AuthorizationUtils.BearerType.AUTHORIZATION_TYPE,
                "",
                sessionManager);

        assertThat(AuthorizationUtils.getAuthentication(request)).isNull();
    }

    @Test
    void authenticatesSessionCookie() {
        String sessionId = "session-cookie";
        InMemorySessionManager sessionManager = new InMemorySessionManager(3600, 100);
        sessionManager.create(sessionId, buildSession(sessionId));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(
                AuthorizationUtils.BearerType.SESSION_COOKIE, sessionId));

        AuthorizationUtils.authenticate(request, sessionManager);

        assertThat(AuthorizationUtils.getAuthentication(request)).isNotNull();
    }

    @Test
    void authenticatesLegacyCongressCookie() {
        String sessionId = "session-legacy";
        InMemorySessionManager sessionManager = new InMemorySessionManager(3600, 100);
        sessionManager.create(sessionId, buildSession(sessionId));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(
                AuthorizationUtils.BearerType.LEGACY_SESSION_COOKIE, sessionId));

        AuthorizationUtils.authenticate(request, sessionManager);

        assertThat(AuthorizationUtils.getAuthentication(request)).isNotNull();
    }

    private Session buildSession(String sessionId) {
        UserInfo user = new UserInfo();
        user.setId("user-1");
        user.setUsername("alice");
        user.setBookId("book-1");
        user.setSessionId(sessionId);

        SignedPrincipal principal = new SignedPrincipal(user, new Session(sessionId));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

        Session session = new Session(sessionId);
        session.setAuthentication(authentication);
        return session;
    }
}
