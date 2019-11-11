package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.Value;
import org.pac4j.core.context.session.SessionStore;

import java.util.Optional;

public class Pac4jSessionStore
    implements org.pac4j.core.context.session.SessionStore<Pac4jContext> {

  private Pac4jContext context;

  public Pac4jSessionStore(Pac4jContext context) {
    this.context = context;
  }

  private Session getSession(Pac4jContext context) {
    return context.getContext().session();
  }

  private Optional<Session> getSessionOrEmpty(Pac4jContext context) {
    return Optional.ofNullable(context.getContext().sessionOrNull());
  }

  @Override public String getOrCreateSessionId(Pac4jContext context) {
    return getSession(context).getId();
  }

  @Override public Optional<Object> get(Pac4jContext context, String key) {
    Optional sessionValue = getSessionOrEmpty(context)
        .map(session -> session.get(key))
        .map(Value::toOptional)
        .orElseGet(Optional::empty);
    return sessionValue;
  }

  @Override public void set(Pac4jContext context, String key, Object value) {
    getSession(context).put(key, value.toString());
  }

  @Override public boolean destroySession(Pac4jContext context) {
    Optional<Session> session = getSessionOrEmpty(context);
    session.ifPresent(Session::destroy);
    return session.isPresent();
  }

  @Override public Optional getTrackableSession(Pac4jContext context) {
    return getSessionOrEmpty(context);
  }

  @Override public Optional<SessionStore<Pac4jContext>> buildFromTrackableSession(
      Pac4jContext context, Object trackableSession) {
    if (trackableSession != null) {
      return Optional.of(new Pac4jSessionStore(context));
    }
    return Optional.empty();
  }

  @Override public boolean renewSession(Pac4jContext context) {
    return false;
  }
}
