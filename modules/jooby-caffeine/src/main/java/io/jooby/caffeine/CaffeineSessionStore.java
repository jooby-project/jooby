/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

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
public class CaffeineSessionStore implements SessionStore {

  private final Cache<String, Session> cache;

  private SessionToken token = SessionToken.cookieId(SessionToken.SID);

  /**
   * Creates a new session store using the given cache.
   *
   * @param cache Cache.
   */
  public CaffeineSessionStore(@Nonnull Cache<String, Session> cache) {
    this.cache = cache;
  }

  /**
   * Creates a new session store with given session timeout.
   *
   * @param timeout Session timeout.
   */
  public CaffeineSessionStore(@Nonnull Duration timeout) {
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

  /**
   * Session token.
   *
   * @return Session token. Uses a cookie by default: {@link SessionToken#SID}.
   */
  public @Nonnull SessionToken getToken() {
    return token;
  }

  /**
   * Set custom session token.
   *
   * @param token Session token.
   * @return This store.
   */
  public @Nonnull CaffeineSessionStore setToken(@Nonnull SessionToken token) {
    this.token = token;
    return this;
  }

  @Nonnull @Override public Session newSession(@Nonnull Context ctx) {
    String sessionId = token.newToken();
    Session session = cache
        .get(sessionId, key -> Session.create(ctx, sessionId, new ConcurrentHashMap<>()));

    token.saveToken(ctx, sessionId);

    return session;
  }

  @Nullable @Override public Session findSession(@Nonnull Context ctx) {
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
    Session session = cache.getIfPresent(sessionId);
    if (session != null) {
      token.saveToken(ctx, sessionId);
    }
    return session;
  }

  @Override public void deleteSession(@Nonnull Context ctx, @Nonnull Session session) {
    cache.invalidate(session.getId());
    token.deleteToken(ctx, session.getId());
  }

  @Override public void touchSession(@Nonnull Context ctx, @Nonnull Session session) {
    token.saveToken(ctx, session.getId());
  }

  @Override public void saveSession(@Nonnull Context ctx, @Nonnull Session session) {
    // NOOP due we do everything on memory
  }

  @Override public void renewSessionId(@Nonnull Context ctx, @Nonnull Session session) {

  }
}
