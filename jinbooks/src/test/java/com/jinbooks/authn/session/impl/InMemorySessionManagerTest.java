package com.jinbooks.authn.session.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.jinbooks.authn.SignedPrincipal;
import com.jinbooks.authn.session.Session;
import com.jinbooks.domain.idm.UserInfo;

class InMemorySessionManagerTest {

	@Test
	void terminateRemovesSession() {
		InMemorySessionManager manager = new InMemorySessionManager(3600, 100);
		Session session = buildSession("session-1", "user-1");
		manager.create("session-1", session);

		manager.terminate("session-1", "user-1", "alice");

		assertThat(manager.get("session-1")).isNull();
	}

	@Test
	void terminateByUserIdRemovesAllUserSessions() {
		InMemorySessionManager manager = new InMemorySessionManager(3600, 100);
		manager.create("s1", buildSession("s1", "user-1"));
		manager.create("s2", buildSession("s2", "user-1"));
		manager.create("s3", buildSession("s3", "user-2"));

		manager.terminateByUserId("user-1");

		assertThat(manager.get("s1")).isNull();
		assertThat(manager.get("s2")).isNull();
		assertThat(manager.get("s3")).isNotNull();
	}

	private Session buildSession(String sessionId, String userId) {
		UserInfo user = new UserInfo();
		user.setId(userId);
		user.setUsername("alice");
		user.setSessionId(sessionId);

		SignedPrincipal principal = new SignedPrincipal(user, new Session(sessionId));
		Session session = new Session(sessionId);
		session.setAuthentication(new UsernamePasswordAuthenticationToken(
				principal, null, Collections.emptyList()));
		return session;
	}
}
