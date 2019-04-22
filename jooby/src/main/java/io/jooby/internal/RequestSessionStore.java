package io.jooby.internal;

import io.jooby.Session;
import io.jooby.SessionStore;

public class RequestSessionStore implements SessionStore {

  private SessionStore store;

  public RequestSessionStore(SessionStore store) {
    this.store = store;
  }

  @Override public Session newSession(String id) {
    return store.newSession(id);
  }

  @Override public Session findSession(String id) {
    return store.findSession(id);
  }

  @Override public void deleteSession(String id) {
    store.deleteSession(id);
  }

  @Override public void save(Session session) {
    store.save(((RequestSession) session).getSession());
  }
}
