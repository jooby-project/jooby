/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.CookieSessionStore;
import io.jooby.internal.MemorySessionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Load and save sessions from store (memory, database, etc.).
 *
 * @author edgar
 * @since 2.0.0
 */
public interface SessionStore {

  /**
   * Creates a new session. This method must:
   *
   * - Set session as new {@link Session#setNew(boolean)}
   * - Optionally, set session creation time {@link Session#setCreationTime(Instant)}
   * - Optionally, set session last accessed time {@link Session#setLastAccessedTime(Instant)}
   *
   * @param ctx Web context.
   * @return A new session.
   */
  @Nonnull Session newSession(@Nonnull Context ctx);

  /**
   * Find an existing session by ID. For existing session this method must:
   *
   * - Optionally, Retrieve/restore session creation time
   * - Optionally, Set session last accessed time {@link Session#setLastAccessedTime(Instant)}
   *
   * @param ctx Web context.
   * @return An existing session or <code>null</code>.
   */
  @Nullable Session findSession(@Nonnull Context ctx);

  /**
   * Delete a session from store. This method must NOT call {@link Session#destroy()}.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void deleteSession(@Nonnull Context ctx, @Nonnull Session session);

  /**
   * Session attributes/state has changed. Every time a session attribute is put or removed it,
   * this method is executed as notification callback.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void touchSession(@Nonnull Context ctx, @Nonnull Session session);

  /**
   * Save a session. This method must save:
   *
   * - Session attributes/data
   * - Optionally set Session metadata like: creationTime, lastAccessed time, etc.
   *
   * This method is call after response is send to client, so context and response shouldn't be
   * modified.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void saveSession(@Nonnull Context ctx, @Nonnull Session session);

  /**
   * Creates a cookie based session and store data in memory. Session data is not keep after
   * restart.
   *
   * It uses the default session cookie: {@link SessionToken#SID}.
   *
   * @return Session store.
   */
  static @Nonnull SessionStore memory() {
    return memory(SessionToken.SID);
  }

  /**
   * Creates a cookie based session and store data in memory. Session data is not keep after
   * restart.
   *
   * @param cookie Cookie to use.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull Cookie cookie) {
    return memory(SessionToken.cookie(cookie));
  }

  /**
   * Creates a session store that save data in memory. Session data is not keep after restart.
   *
   * @param token Session token.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull SessionToken token) {
    return new MemorySessionStore(token);
  }

  /**
   * Creates a session store that save data into Cookie. Cookie data is signed it using
   * <code>HMAC_SHA256</code>. See {@link Cookie#sign(String, String)}.
   *
   * @param secret Secret token to signed data.
   * @param cookie Cookie to use.
   * @return A browser session store.
   */
  static @Nonnull SessionStore cookie(@Nonnull String secret, @Nonnull Cookie cookie) {
    return new CookieSessionStore(secret, cookie);
  }

  /**
   * Creates a session store that save data into Cookie. Cookie data is signed it using
   * <code>HMAC_SHA256</code>. See {@link Cookie#sign(String, String)}.
   *
   * It uses the default session cookie: {@link SessionToken#SID}.
   *
   * @param secret Secret token to signed data.
   * @return A browser session store.
   */
  static @Nonnull SessionStore cookie(@Nonnull String secret) {
    return cookie(secret, SessionToken.SID);
  }
}
