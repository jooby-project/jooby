/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.InMemorySessionStore;
import io.jooby.internal.MultipleSessionId;
import io.jooby.internal.SecretSessionId;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Options for HTTP session. Allows provides a session ID generator, session store and configure
 * cookie details, like: name, max-age, path, etc.
 *
 * Uses a memory session store, which you should combine with an a sticky sessions proxy if you
 * plan to run multiple instances.
 *
 * @author edgar
 * @since 2.0.0
 */
public class SessionOptions {
  private static final Cookie DEFAULT_COOKIE = new Cookie("jooby.sid")
      .setMaxAge(Duration.ofSeconds(-1))
      .setHttpOnly(true)
      .setPath("/");

  private static final int ID_SIZE = 30;

  private static final SecureRandom secure = new SecureRandom();

  private SessionStore store = new InMemorySessionStore();

  private final SessionId sessionId;

  /**
   * Creates a session options.
   *
   * @param secret Secret key. Used to signed the cookie.
   * @param sessionId session ID.
   */
  public SessionOptions(@Nonnull String secret, @Nonnull SessionId... sessionId) {
    this.sessionId = new SecretSessionId(createSessionId(sessionId), secret);
  }

  /**
   * Creates a session options.
   *
   * @param sessionId session ID.
   */
  public SessionOptions(@Nonnull SessionId... sessionId) {
    this.sessionId = createSessionId(sessionId);
  }

  /**
   * Session ID strategy (cookie or header).
   *
   * @return Session ID strategy (cookie or header).
   */
  public SessionId getSessionId() {
    return sessionId;
  }

  /**
   * Session store (defaults uses memory).
   *
   * @return Session store (defaults uses memory).
   */
  public @Nonnull SessionStore getStore() {
    return store;
  }

  /**
   * Set session store.
   *
   * @param store Session store.
   * @return This options.
   */
  public @Nonnull SessionOptions setStore(@Nonnull SessionStore store) {
    this.store = store;
    return this;
  }

  /**
   * Generates a Session ID.
   *
   * @return Session ID.
   */
  public @Nonnull String generateId() {
    byte[] bytes = new byte[ID_SIZE];
    secure.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static SessionId createSessionId(@Nonnull SessionId[] sessionId) {
    if (sessionId.length == 0) {
      return SessionId.cookie(DEFAULT_COOKIE);
    } else if (sessionId.length == 1) {
      return sessionId[0];
    } else {
      return new MultipleSessionId(sessionId);
    }
  }
}
