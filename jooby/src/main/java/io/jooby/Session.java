package io.jooby;

import io.jooby.internal.RequestSession;
import io.jooby.internal.SessionImpl;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface Session {
  @Nonnull String getId();

  @Nonnull Value get(@Nonnull String name);

  @Nonnull Session put(@Nonnull String name, int value);

  @Nonnull Session put(@Nonnull String name, long value);

  @Nonnull Session put(@Nonnull String name, CharSequence value);

  @Nonnull Session put(@Nonnull String name, String value);

  @Nonnull Session put(@Nonnull String name, float value);

  @Nonnull Session put(@Nonnull String name, double value);

  @Nonnull Session put(@Nonnull String name, boolean value);

  @Nonnull Session put(@Nonnull String name, Number value);

  @Nonnull Value remove(@Nonnull String name);

  @Nonnull Map<String, String> toMap();

  @Nonnull Instant getCreationTime();

  @Nonnull Session setCreationTime(@Nonnull Instant creationTime);

  @Nonnull Instant getLastAccessedTime();

  @Nonnull Duration getMaxInactiveInterval();

  @Nonnull Session setLastAccessedTime(@Nonnull Instant lastAccessedTime);

  boolean isNew();

  @Nonnull Session setNew(boolean isNew);

  boolean isModify();

  @Nonnull Session setModify(boolean modify);

  void destroy();

  static Session create(String id) {
    return new SessionImpl(id);
  }

  static Session create(Context context, Session session) {
    return new RequestSession(context, session);
  }
}
