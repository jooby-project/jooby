package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionOptions;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class RequestSession implements Session {

  private Context context;

  private Session session;

  public RequestSession(Context context, Session session) {
    this.context = context;
    this.session = session;
  }

  @Nonnull @Override public String getId() {
    return session.getId();
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return session.get(name);
  }

  @Nonnull @Override public Session put(@Nonnull String name, int value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, long value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, CharSequence value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, String value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, float value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, double value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Session put(@Nonnull String name, boolean value) {
    return null;
  }

  @Nonnull @Override public Session put(@Nonnull String name, Number value) {
    session.put(name, value);
    return this;
  }

  @Nonnull @Override public Value remove(@Nonnull String name) {
    return session.remove(name);
  }

  @Nonnull @Override public Map<String, String> toMap() {
    return session.toMap();
  }

  @Nonnull @Override public Instant getCreationTime() {
    return session.getCreationTime();
  }

  @Nonnull @Override public Instant getLastAccessedTime() {
    return session.getLastAccessedTime();
  }

  @Nonnull @Override public Duration getMaxInactiveInterval() {
    return session.getMaxInactiveInterval();
  }

  @Nonnull @Override public Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    session.setLastAccessedTime(lastAccessedTime);
    return this;
  }

  public void destroy() {
    if (context != null) {
      try {
        SessionOptions options = context.getRouter().getSessionOptions();
        context.setResponseCookie(options.getCookie().setMaxAge(0));
        options.getStore().deleteSession(session.getId());
      } finally {
        context = null;
        session = null;
      }
    }
  }
}
