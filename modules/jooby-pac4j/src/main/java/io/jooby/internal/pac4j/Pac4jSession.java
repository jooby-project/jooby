/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.time.Instant;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.*;
import io.jooby.pac4j.Pac4jUntrustedDataFound;

class Pac4jSession implements Session {
  public static final String PAC4J = "p4j~";

  public static final String BIN = "b64~";

  private final Session session;

  public Pac4jSession(@NonNull Session session) {
    this.session = session;
  }

  @Nullable public String getId() {
    return session.getId();
  }

  @NonNull public Value get(@NonNull String name) {
    return session.get(name);
  }

  @NonNull public Instant getLastAccessedTime() {
    return session.getLastAccessedTime();
  }

  public void destroy() {
    session.destroy();
  }

  @NonNull public Session setId(String id) {
    session.setId(id);
    return this;
  }

  @NonNull public Value remove(@NonNull String name) {
    return session.remove(name);
  }

  public boolean isNew() {
    return session.isNew();
  }

  @NonNull public Session setNew(boolean isNew) {
    session.setNew(isNew);
    return this;
  }

  @NonNull public Session setLastAccessedTime(@NonNull Instant lastAccessedTime) {
    session.setLastAccessedTime(lastAccessedTime);
    return this;
  }

  public boolean isModify() {
    return session.isModify();
  }

  @NonNull public Session setCreationTime(@NonNull Instant creationTime) {
    session.setCreationTime(creationTime);
    return this;
  }

  @NonNull public Session setModify(boolean modify) {
    session.setModify(modify);
    return this;
  }

  public Session renewId() {
    session.renewId();
    return this;
  }

  @NonNull public Instant getCreationTime() {
    return session.getCreationTime();
  }

  @NonNull public Map<String, String> toMap() {
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
      @NonNull @Override
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

  @NonNull @Override
  public Session put(@NonNull String name, @NonNull String value) {
    if (value != null) {
      if (value.startsWith(PAC4J) || value.startsWith(BIN)) {
        throw new Pac4jUntrustedDataFound(name);
      }
    }
    return session.put(name, value);
  }
}
