/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemorySessionStore implements SessionStore {
  private static class SessionData {
    private Instant lastAccessedTime;
    private Instant creationTime;
    private Map hash;

    public SessionData(Instant creationTime, Instant lastAccessedTime, Map hash) {
      this.creationTime = creationTime;
      this.lastAccessedTime = lastAccessedTime;
      this.hash = hash;
    }
  }

  private ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();

  private SessionToken token;

  private Duration timeout;

  public MemorySessionStore(SessionToken token, Duration timeout) {
    this.token = token;
    this.timeout = Optional.ofNullable(timeout)
        .filter(t -> t.toMillis() > 0)
        .orElse(null);
  }

  @Override public Session newSession(Context ctx) {
    String sessionId = token.newToken();
    SessionData data = sessions.computeIfAbsent(sessionId, sid -> {
      Instant now = Instant.now();
      return new SessionData(now, now, new ConcurrentHashMap());
    });

    Session session = restore(ctx, sessionId, data);

    token.saveToken(ctx, sessionId);
    return session;
  }

  @Override public Session findSession(Context ctx) {
    purge();
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
    SessionData data = sessions.get(sessionId);
    if (data != null) {
      Session session = restore(ctx, sessionId, data);
      token.saveToken(ctx, sessionId);
      return session;
    }
    return null;
  }

  /**
   * Check for expired session and delete them.
   */
  private void purge() {
    if (timeout != null) {
      Iterator<Map.Entry<String, SessionData>> iterator = sessions.entrySet().iterator();
      Instant now = Instant.now();
      while (iterator.hasNext()) {
        Map.Entry<String, SessionData> entry = iterator.next();
        SessionData session = entry.getValue();
        Duration timeElapsed = Duration.between(session.lastAccessedTime, now);
        if (timeElapsed.compareTo(timeout) > 0) {
          iterator.remove();
        }
      }
    }
  }

  @Override public void deleteSession(@Nonnull Context ctx, @Nonnull Session session) {
    String sessionId = session.getId();
    sessions.remove(sessionId);
    token.deleteToken(ctx, sessionId);
  }

  @Override public void touchSession(@Nonnull Context ctx, @Nonnull Session session) {
    saveSession(ctx, session);
    token.saveToken(ctx, session.getId());
  }

  @Override public void saveSession(Context ctx, @Nonnull Session session) {
    String sessionId = session.getId();
    sessions.put(sessionId,
        new SessionData(session.getCreationTime(), Instant.now(), session.toMap()));
  }

  @Override public void renewSessionId(@Nonnull Context ctx, @Nonnull Session session) {
    String oldId = session.getId();
    String newId = token.newToken();
    session.setId(newId);
    SessionData data = sessions.remove(oldId);
    sessions.put(newId, data);
  }

  public SessionStore setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  private Session restore(Context ctx, String sessionId, SessionData data) {
    return Session.create(ctx, sessionId, data.hash)
        .setLastAccessedTime(data.lastAccessedTime)
        .setCreationTime(data.creationTime);
  }
}
