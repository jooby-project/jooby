/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.SignedSessionStore;
import io.jooby.internal.MemorySessionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

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
    return memory(SessionToken.cookieId(cookie));
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
   * Creates a session store that uses (un)signed data. Session data is signed it using
   * <code>HMAC_SHA256</code>.
   *
   * See {@link Cookie#sign(String, String)} and {@link Cookie#unsign(String, String)}.
   *
   * @param secret Secret token to signed data.
   * @return A browser session store.
   */
  static @Nonnull SessionStore signed(@Nonnull String secret) {
    return signed(secret, SessionToken.SID);
  }

  /**
   * Creates a session store that uses (un)signed data. Session data is signed it using
   * <code>HMAC_SHA256</code>.
   *
   * See {@link Cookie#sign(String, String)} and {@link Cookie#unsign(String, String)}.
   *
   * @param secret Secret token to signed data.
   * @param cookie Cookie to use.
   * @return A browser session store.
   */
  static @Nonnull SessionStore signed(@Nonnull String secret, @Nonnull Cookie cookie) {
    return signed(secret, SessionToken.signedCookie(cookie));
  }

  /**
   * Creates a session store that uses (un)signed data. Session data is signed it using
   * <code>HMAC_SHA256</code>.
   *
   * See {@link Cookie#sign(String, String)} and {@link Cookie#unsign(String, String)}.
   *
   * @param secret Secret token to signed data.
   * @param token Session token to use.
   * @return A browser session store.
   */
  static @Nonnull SessionStore signed(@Nonnull String secret, @Nonnull SessionToken token) {
    SneakyThrows.Function<String, Map<String, String>> decoder = value -> {
      String unsign = Cookie.unsign(value, secret);
      if (unsign == null) {
        return null;
      }
      return Cookie.decode(unsign);
    };

    SneakyThrows.Function<Map<String, String>, String> encoder = attributes ->
        Cookie.sign(Cookie.encode(attributes), secret);

    return signed(token, decoder, encoder);
  }

  /**
   * Creates a session store that save data into Cookie. Cookie data is (un)signed it using the given
   * decoder and encoder.
   *
   * @param token Token to use.
   * @param decoder Decoder to use.
   * @param encoder Encoder to use.
   * @return Cookie session store.
   */
  static @Nonnull SessionStore signed(@Nonnull SessionToken token,
      @Nonnull Function<String, Map<String, String>> decoder,
      @Nonnull Function<Map<String, String>, String> encoder) {
    return new SignedSessionStore(token, decoder, encoder);
  }
}
