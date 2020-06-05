/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jooby.SessionStore;
import io.jooby.SessionToken;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.function.Function;

/**
 * Caffeine session store.
 *
 * Usage:
 * <pre>{@code
 * {
 *   setSessionStore(new CaffeineSessionStore());
 * }
 * }</pre>
 *
 * Default session timeout is: <code>30 minutes</code>.
 *
 * @author edgar
 * @since 2.8.5
 */
public class CaffeineSessionStore extends SessionStore.InMemory {

  private final Cache<String, Object> cache;

  /**
   * Creates a new session store using the given cache.
   *
   * @param cache Cache.
   */
  public CaffeineSessionStore(@Nonnull Cache<String, Object> cache) {
    super(SessionToken.cookieId(SessionToken.SID));
    this.cache = cache;
  }

  /**
   * Creates a new session store with given session timeout.
   *
   * @param timeout Session timeout.
   */
  public CaffeineSessionStore(@Nonnull Duration timeout) {
    super(SessionToken.cookieId(SessionToken.SID));
    this.cache = Caffeine.newBuilder()
        .expireAfterAccess(timeout)
        .build();
  }

  /**
   * Creates a new session store with timeout of <code>30 minutes</code>.
   */
  public CaffeineSessionStore() {
    this(Duration.ofMinutes(DEFAULT_TIMEOUT));
  }

  @Override protected Data getOrCreate(String sessionId,
      Function<String, Data> factory) {
    return (Data) cache.get(sessionId, factory);
  }

  @Override protected Data getOrNull(String sessionId) {
    return (Data) cache.getIfPresent(sessionId);
  }

  @Override protected Data remove(String sessionId) {
    Data data = (Data) cache.getIfPresent(sessionId);
    cache.invalidate(sessionId);
    return data;
  }

  @Override protected void put(String sessionId, Data data) {
    cache.put(sessionId, data);
  }
}
