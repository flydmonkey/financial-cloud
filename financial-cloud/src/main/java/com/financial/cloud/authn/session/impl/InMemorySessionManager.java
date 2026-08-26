package com.financial.cloud.authn.session.impl;


import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.authn.session.Session;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.authn.session.UserSessions;
import com.financial.cloud.domain.permissions.SessionList;

import com.financial.cloud.authn.core.AuthAuthentication;

@Slf4j
public class InMemorySessionManager implements SessionManager {

	private final int validitySeconds;
	private final int maxSize;

	private final Cache<String, Session> sessionStore;
	private final Cache<String, Session> sessionTwoFactorStore;
	private final Cache<String, UserSessions> userSessionsStore;

	public InMemorySessionManager(int validitySeconds, int maxSize) {
		this.validitySeconds = validitySeconds;
		this.maxSize = maxSize;
		sessionStore = Caffeine.newBuilder()
				.expireAfterWrite(validitySeconds, TimeUnit.SECONDS)
				.maximumSize(maxSize)
				.build();

		sessionTwoFactorStore = Caffeine.newBuilder()
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.maximumSize(maxSize)
				.build();

		userSessionsStore = Caffeine.newBuilder()
				.expireAfterWrite(validitySeconds, TimeUnit.SECONDS)
				.maximumSize(maxSize)
				.build();

		log.info("In-memory session store ready (maxSize={}, validitySeconds={})", maxSize, validitySeconds);
	}

	@Override
	public void create(String sessionId, Session session) {
		session.setExpiredTime(session.getLastAccessTime().plusSeconds(validitySeconds));
		sessionStore.put(sessionId, session);
	}

	@Override
	public Session remove(String sessionId) {
		Session session = sessionStore.getIfPresent(sessionId);
		sessionStore.invalidate(sessionId);
		return session;
	}

	@Override
	public Session get(String sessionId) {
		return sessionStore.getIfPresent(sessionId);
	}

	@Override
	public Session refresh(String sessionId, LocalDateTime refreshTime) {
		Session session = get(sessionId);
		if (session != null) {
			log.debug("refresh session Id {} at refreshTime {}", sessionId, refreshTime);
			session.setLastAccessTime(refreshTime);
			create(sessionId, session);
		}
		return session;
	}

	@Override
	public Session refresh(String sessionId) {
		Session session = get(sessionId);
		if (session != null) {
			LocalDateTime currentTime = LocalDateTime.now();
			log.debug("refresh session Id {} at time {}", sessionId, currentTime);
			session.setLastAccessTime(currentTime);
			create(sessionId, session);
		}
		return session;
	}

	@Override
	public int getValiditySeconds() {
		return validitySeconds;
	}

	@Override
	public List<SessionList> sessionList(String style) {
		return Collections.emptyList();
	}

	@Override
	public void terminate(String sessionId, String userId, String username) {
		log.debug("terminate session {} for user {}({})", sessionId, username, userId);
		remove(sessionId);
	}

	@Override
	public void terminateByUserId(String userId) {
		if (userId == null) {
			return;
		}
		ConcurrentMap<String, Session> sessions = sessionStore.asMap();
		List<String> toRemove = new ArrayList<>();
		for (Map.Entry<String, Session> entry : sessions.entrySet()) {
			AuthAuthentication authentication = entry.getValue().getAuthentication();
			if (authentication == null) {
				continue;
			}
			Object principal = authentication.getPrincipal();
			if (principal instanceof SignedPrincipal signedPrincipal
					&& userId.equals(signedPrincipal.getUserId())) {
				toRemove.add(entry.getKey());
			}
		}
		toRemove.forEach(this::remove);
		log.debug("terminated {} session(s) for user {}", toRemove.size(), userId);
	}

	@Override
	public void setLimit(int sessionLimit) {
		log.debug("session limit {} ignored for in-memory store (use jinbooks.session.max-size)", sessionLimit);
	}

	@Override
	public void put(String style, String userId, String sessionKey) {
		log.trace("session index put ignored for style={}, userId={}", style, userId);
	}
}
