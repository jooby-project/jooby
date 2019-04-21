package io.jooby;

import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore implements SessionStore {
  private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

  @Override public Session newSession(String id) {
    return Session.create(id);
  }

  @Override public Session findSession(String id) {
    return sessions.get(id);
  }

  @Override public void deleteSession(String id) {
    Session session = sessions.remove(id);
    if (session != null) {
      session.destroy();
    }
  }

  @Override public void save(Session session) {
    sessions.put(session.getId(), session);
  }
}
