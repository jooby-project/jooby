/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.MemorySessionStore;
import io.jooby.internal.SignedSessionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Load and save sessions from store (memory, database, etc.).
 *
 * @author edgar
 * @since 2.0.0
 */
public interface SessionStore {

  /** Default session timeout in minutes. */
  int DEFAULT_TIMEOUT = 30;

  /**
   * Base class for in-memory session store.
   *
   * @author edgar.
   * @since 2.0.0
   */
  abstract class InMemory implements SessionStore {
    protected static class Data {
      private Instant lastAccessedTime;
      private Instant creationTime;
      private Map hash;

      public Data(Instant creationTime, Instant lastAccessedTime, Map hash) {
        this.creationTime = creationTime;
        this.lastAccessedTime = lastAccessedTime;
        this.hash = hash;
      }

      public boolean isExpired(Duration timeout) {
        Duration timeElapsed = Duration.between(lastAccessedTime, Instant.now());
        return timeElapsed.compareTo(timeout) > 0;
      }
    }

    private SessionToken token;

    /**
     * Creates a new in-memory session store.
     *
     * @param token Token.
     */
    protected InMemory(@Nonnull SessionToken token) {
      this.token = token;
    }

    @Override public @Nonnull Session newSession(@Nonnull Context ctx) {
      String sessionId = token.newToken();
      Data data = getOrCreate(sessionId,
          sid -> new Data(Instant.now(), Instant.now(), new ConcurrentHashMap()));

      Session session = restore(ctx, sessionId, data);

      token.saveToken(ctx, sessionId);
      return session;
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
    public @Nonnull SessionStore setToken(@Nonnull SessionToken token) {
      this.token = token;
      return this;
    }

    protected abstract @Nonnull Data getOrCreate(@Nonnull String sessionId,
        @Nonnull Function<String, Data> factory);

    protected abstract @Nullable Data getOrNull(@Nonnull String sessionId);

    protected abstract @Nullable Data remove(@Nonnull String sessionId);

    protected abstract void put(@Nonnull String sessionId, @Nonnull Data data);

    @Override public Session findSession(Context ctx) {
      String sessionId = token.findToken(ctx);
      if (sessionId == null) {
        return null;
      }
      Data data = getOrNull(sessionId);
      if (data != null) {
        Session session = restore(ctx, sessionId, data);
        token.saveToken(ctx, sessionId);
        return session;
      }
      return null;
    }

    @Override public void deleteSession(@Nonnull Context ctx, @Nonnull Session session) {
      String sessionId = session.getId();
      remove(sessionId);
      token.deleteToken(ctx, sessionId);
    }

    @Override public void touchSession(@Nonnull Context ctx, @Nonnull Session session) {
      saveSession(ctx, session);
      token.saveToken(ctx, session.getId());
    }

    @Override public void saveSession(Context ctx, @Nonnull Session session) {
      String sessionId = session.getId();
      put(sessionId, new Data(session.getCreationTime(), Instant.now(), session.toMap()));
    }

    @Override public void renewSessionId(@Nonnull Context ctx, @Nonnull Session session) {
      String oldId = session.getId();
      Data data = remove(oldId);
      if (data != null) {
        String newId = token.newToken();
        session.setId(newId);

        put(newId, data);
      }
    }

    private Session restore(Context ctx, String sessionId, Data data) {
      return Session.create(ctx, sessionId, data.hash)
          .setLastAccessedTime(data.lastAccessedTime)
          .setCreationTime(data.creationTime);
    }
  }

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
   * Renew Session ID. This operation might or might not be implemented by a Session Store.
   *
   * @param ctx Web Context.
   * @param session Session.
   */
  void renewSessionId(@Nonnull Context ctx, @Nonnull Session session);

  /**
   * Creates a cookie based session and store data in memory. Session data is not keep after
   * restart.
   *
   * It uses the default session cookie: {@link SessionToken#SID}.
   *
   * - Session data is not keep after restart.
   *
   * @param timeout Timeout in seconds. Use <code>-1</code> for no timeout.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(int timeout) {
    return memory(SessionToken.SID, Duration.ofSeconds(timeout));
  }

  /**
   * Creates a cookie based session and store data in memory. Session data is not keep after
   * restart.
   *
   * It uses the default session cookie: {@link SessionToken#SID}.
   *
   * - Session expires after 30 minutes of inactivity.
   * - Session data is not keep after restart.
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
   * It uses the default session cookie: {@link SessionToken#SID}.
   *
   * @param timeout Expires session after amount of inactivity time.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull Duration timeout) {
    return memory(SessionToken.SID, timeout);
  }

  /**
   * Creates a cookie based session and store data in memory.
   *
   * - Session expires after 30 minutes of inactivity.
   * - Session data is not keep after restart.
   *
   * @param cookie Cookie to use.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull Cookie cookie) {
    return memory(SessionToken.cookieId(cookie));
  }

  /**
   * Creates a cookie based session and store data in memory. Session data is not keep after
   * restart.
   *
   * @param cookie Cookie to use.
   * @param timeout Expires session after amount of inactivity time.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull Cookie cookie, @Nonnull Duration timeout) {
    return memory(SessionToken.cookieId(cookie), timeout);
  }

  /**
   * Creates a session store that save data in memory.
   * - Session expires after 30 minutes of inactivity.
   * - Session data is not keep after restart.
   *
   * @param token Session token.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull SessionToken token) {
    return new MemorySessionStore(token, Duration.ofMinutes(DEFAULT_TIMEOUT));
  }

  /**
   * Creates a session store that save data in memory. Session data is not keep after restart.
   *
   * @param token Session token.
   * @param timeout Expires session after amount of inactivity time.
   * @return Session store.
   */
  static @Nonnull SessionStore memory(@Nonnull SessionToken token, @Nonnull Duration timeout) {
    return new MemorySessionStore(token, timeout);
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
