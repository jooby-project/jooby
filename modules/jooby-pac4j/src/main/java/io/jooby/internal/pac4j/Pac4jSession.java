package io.jooby.internal.pac4j;

import io.jooby.*;
import io.jooby.pac4j.Pac4jUntrustedDataFound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

class Pac4jSession  implements Session {
  public static final String PAC4J = "p4j~";

  public static final String BIN = "b64~";

  private final Session session;

  public Pac4jSession(@Nonnull Session session) {
    this.session = session;
  }

  @Nullable
  public String getId() {
    return session.getId();
  }

  @Nonnull
  public Value get(@Nonnull String name) {
    return session.get(name);
  }

  @Nonnull
  public Instant getLastAccessedTime() {
    return session.getLastAccessedTime();
  }

  public void destroy() {
    session.destroy();
  }

  @Nonnull
  public Session setId(String id) {
    session.setId(id);
    return this;
  }

  @Nonnull
  public Value remove(@Nonnull String name) {
    return session.remove(name);
  }

  public boolean isNew() {
    return session.isNew();
  }

  @Nonnull
  public Session setNew(boolean isNew) {
    session.setNew(isNew);
    return this;
  }

  @Nonnull
  public Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    session.setLastAccessedTime(lastAccessedTime);
    return this;
  }

  public boolean isModify() {
    return session.isModify();
  }

  @Nonnull
  public Session setCreationTime(@Nonnull Instant creationTime) {
    session.setCreationTime(creationTime);
    return this;
  }

  @Nonnull
  public Session setModify(boolean modify) {
    session.setModify(modify);
    return this;
  }

  public Session renewId() {
    session.renewId();
    return this;
  }

  @Nonnull
  public Instant getCreationTime() {
    return session.getCreationTime();
  }

  @Nonnull
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
      @Nonnull
      @Override
      public Session session() {
        return new Pac4jSession(super.session());
      }

      @Override
      public Session sessionOrNull() {
        Session session =  super.sessionOrNull();
        return session == null ? null : new Pac4jSession(session);
      }
    };
  }

  @Nonnull
  @Override
  public Session put(@Nonnull String name, @Nonnull String value) {
    if (value != null) {
      if (value.startsWith(PAC4J) || value.startsWith(BIN)) {
        throw new Pac4jUntrustedDataFound(name);
      }
    }
    return session.put(name, value);
  }
}
