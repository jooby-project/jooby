/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Session;
import io.jooby.SessionStore;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {
  private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

  @Override public Session newSession(String id) {
    Instant now = Instant.now();
    Session session = Session.create(id)
        .setCreationTime(now)
        .setLastAccessedTime(now)
        .setNew(true);
    return session;
  }

  @Override public Session findSession(String id) {
    Session session = sessions.get(id);
    if (session != null) {
      session.setLastAccessedTime(Instant.now());
    }
    return session;
  }

  @Override public void deleteSession(String id) {
    sessions.remove(id);
  }

  @Override public void save(Session session) {
    sessions.put(session.getId(), session);
    session
        .setNew(false)
        .setModify(false)
        .setLastAccessedTime(Instant.now());
  }
}
