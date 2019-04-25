/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.InMemorySessionStore;
import io.jooby.internal.RequestSessionStore;

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

  private SessionId[] sessionId;

  /**
   * Creates a session options.
   *
   * @param sessionId session ID.
   */
  public SessionOptions(@Nonnull SessionId... sessionId) {
    this.sessionId = sessionId.length == 0
        ? new SessionId[]{SessionId.cookie(DEFAULT_COOKIE)}
        : sessionId;
  }

  public SessionId[] getSessionId() {
    return sessionId;
  }

  /**
   * Session store (defaults uses memory).
   *
   * @return Session store (defaults uses memory).
   */
  public @Nonnull SessionStore getStore() {
    return new RequestSessionStore(store);
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

}
