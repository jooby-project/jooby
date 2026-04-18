/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.pac4j.Pac4jUntrustedDataFound;
import io.jooby.value.Value;

class Pac4jSession implements Session {
  public static final String PAC4J = "p4j~";

  public static final String BIN = "b64~";

  private final Session session;

  public Pac4jSession(Session session) {
    this.session = session;
  }

  @Nullable public String getId() {
    return session.getId();
  }

  public Value get(String name) {
    return session.get(name);
  }

  public Instant getLastAccessedTime() {
    return session.getLastAccessedTime();
  }

  public void destroy() {
    session.destroy();
  }

  public Session setId(String id) {
    session.setId(id);
    return this;
  }

  public Value remove(String name) {
    return session.remove(name);
  }

  public boolean isNew() {
    return session.isNew();
  }

  public Session setNew(boolean isNew) {
    session.setNew(isNew);
    return this;
  }

  public Session setLastAccessedTime(Instant lastAccessedTime) {
    session.setLastAccessedTime(lastAccessedTime);
    return this;
  }

  public boolean isModify() {
    return session.isModify();
  }

  public Session setCreationTime(Instant creationTime) {
    session.setCreationTime(creationTime);
    return this;
  }

  public Session setModify(boolean modify) {
    session.setModify(modify);
    return this;
  }

  public Session renewId() {
    session.renewId();
    return this;
  }

  public Instant getCreationTime() {
    return session.getCreationTime();
  }

  public Map<String, String> toMap() {
    return session.toMap();
  }

  public Session clear() {
    session.clear();
    return this;
  }

  public Session getSession() {
    return session;
  }

  public static Context create(Context ctx) {
    return new ForwardingContext(ctx) {
      @Override
      public Session session() {
        return new Pac4jSession(super.session());
      }

      @Override
      public Session sessionOrNull() {
        Session session = super.sessionOrNull();
        return session == null ? null : new Pac4jSession(session);
      }
    };
  }

  @Override
  public Session put(String name, String value) {
    if (value != null) {
      if (value.startsWith(PAC4J) || value.startsWith(BIN)) {
        throw new Pac4jUntrustedDataFound(name);
      }
    }
    return session.put(name, value);
  }
}
