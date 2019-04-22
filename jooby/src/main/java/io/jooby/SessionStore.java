package io.jooby;

public interface SessionStore {

  Session newSession(String id);

  Session findSession(String id);

  void deleteSession(String id);

  void save(Session session);
}
