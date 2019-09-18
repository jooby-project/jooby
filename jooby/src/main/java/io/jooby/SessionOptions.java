/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
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
  private static final int ID_SIZE = 30;

  private static final SecureRandom secure = new SecureRandom();

  private final SessionStore store;

  public SessionOptions(SessionStore store) {
    this.store = store;
  }

  public SessionOptions() {
    this(SessionStore.memory());
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
   * Generates a Session ID.
   *
   * @return Session ID.
   */
  public @Nonnull String generateId() {
    byte[] bytes = new byte[ID_SIZE];
    secure.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
