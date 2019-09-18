/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.SessionToken;
import io.jooby.SessionOptions;
import io.jooby.SessionStore;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
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

  public MemorySessionStore(SessionToken token) {
    this.token = token;
  }

  @Nonnull @Override public SessionToken getSessionToken() {
    return token;
  }

  @Override public Session newSession(Context ctx) {
    SessionOptions options = sessionOptions(ctx);
    String sessionId = options.generateId();
    Instant now = Instant.now();
    Session session = Session.create(ctx, sessionId)
        .setCreationTime(now)
        .setLastAccessedTime(now)
        .setNew(true);
    token.saveToken(ctx, sessionId);
    return session;
  }

  @Override public Session findSession(Context ctx) {
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
    SessionData data = sessions.get(sessionId);
    if (data != null) {
      Session session = Session.create(ctx, sessionId, data.hash);
      session.setLastAccessedTime(data.lastAccessedTime);
      session.setCreationTime(data.creationTime);
      token.saveToken(ctx, sessionId);
      return session;
    }
    return null;
  }

  @Override public void deleteSession(Context ctx) {
    String sessionId = ctx.session().getId();
    sessions.remove(sessionId);
    token.deleteToken(ctx, sessionId);
  }

  @Override public void save(Context ctx) {
    Session session = ctx.session();
    String sessionId = ctx.session().getId();
    sessions.put(sessionId,
        new SessionData(session.getCreationTime(), Instant.now(), session.toMap()));
    //    session
    //        .setNew(false)
    //        .setModify(false)
    //        .setLastAccessedTime(Instant.now());
  }

  private static SessionOptions sessionOptions(Context ctx) {
    Router router = ctx.getRouter();
    return router.getSessionOptions();
  }
}
