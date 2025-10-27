/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.MemorySessionStore;
import io.jooby.internal.SignedSessionStore;

/**
 * Load and save sessions from store (memory, database, etc.).
 *
 * @author edgar
 * @since 2.0.0
 */
public interface SessionStore {

  /** Default session timeout in minutes. */
  int DEFAULT_TIMEOUT = 30;

  /** Default session, which is the same as no session installed. */
  SessionStore UNSUPPORTED =
      new SessionStore() {

        @Override
        public Session newSession(@NonNull Context ctx) {
          throw Usage.noSession();
        }

        @Nullable @Override
        public Session findSession(@NonNull Context ctx) {
          throw Usage.noSession();
        }

        @Override
        public void deleteSession(@NonNull Context ctx, @NonNull Session session) {
          throw Usage.noSession();
        }

        @Override
        public void touchSession(@NonNull Context ctx, @NonNull Session session) {
          throw Usage.noSession();
        }

        @Override
        public void saveSession(@NonNull Context ctx, @NonNull Session session) {
          throw Usage.noSession();
        }

        @Override
        public void renewSessionId(@NonNull Context ctx, @NonNull Session session) {
          throw Usage.noSession();
        }
      };

  /**
   * Base class for in-memory session store.
   *
   * @author edgar.
   * @since 2.0.0
   */
  abstract class InMemory implements SessionStore {
    protected static class Data {
      private final Instant lastAccessedTime;
      private final Instant creationTime;
      private final Map hash;

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
    protected InMemory(@NonNull SessionToken token) {
      this.token = token;
    }

    @Override
    public @NonNull Session newSession(@NonNull Context ctx) {
      var sessionId = token.newToken();
      var data =
          getOrCreate(
              sessionId, sid -> new Data(Instant.now(), Instant.now(), new ConcurrentHashMap()));

      var session = restore(ctx, sessionId, data);

      token.saveToken(ctx, sessionId);
      return session;
    }

    /**
     * Session token.
     *
     * @return Session token.
     */
    public @NonNull SessionToken getToken() {
      return token;
    }

    /**
     * Set custom session token.
     *
     * @param token Session token.
     * @return This store.
     */
    public @NonNull SessionStore setToken(@NonNull SessionToken token) {
      this.token = token;
      return this;
    }

    protected abstract Data getOrCreate(
        @NonNull String sessionId, @NonNull Function<String, Data> factory);

    protected abstract @Nullable Data getOrNull(@NonNull String sessionId);

    protected abstract @Nullable Data remove(@NonNull String sessionId);

    protected abstract void put(@NonNull String sessionId, @NonNull Data data);

    @Override
    public @Nullable Session findSession(@NonNull Context ctx) {
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

    @Override
    public void deleteSession(@NonNull Context ctx, @NonNull Session session) {
      String sessionId = session.getId();
      remove(sessionId);
      token.deleteToken(ctx, sessionId);
    }

    @Override
    public void touchSession(@NonNull Context ctx, @NonNull Session session) {
      saveSession(ctx, session);
      token.saveToken(ctx, session.getId());
    }

    @Override
    public void saveSession(@NonNull Context ctx, @NonNull Session session) {
      String sessionId = session.getId();
      put(sessionId, new Data(session.getCreationTime(), Instant.now(), session.toMap()));
    }

    @Override
    public void renewSessionId(@NonNull Context ctx, @NonNull Session session) {
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
   * <p>- Set session as new {@link Session#setNew(boolean)} - Optionally, set session creation time
   * {@link Session#setCreationTime(Instant)} - Optionally, set session last accessed time {@link
   * Session#setLastAccessedTime(Instant)}
   *
   * @param ctx Web context.
   * @return A new session.
   */
  @NonNull Session newSession(@NonNull Context ctx);

  /**
   * Find an existing session by ID. For existing session this method must:
   *
   * <p>- Optionally, Retrieve/restore session creation time - Optionally, Set session last accessed
   * time {@link Session#setLastAccessedTime(Instant)}
   *
   * @param ctx Web context.
   * @return An existing session or <code>null</code>.
   */
  @Nullable Session findSession(@NonNull Context ctx);

  /**
   * Delete a session from store. This method must NOT call {@link Session#destroy()}.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void deleteSession(@NonNull Context ctx, @NonNull Session session);

  /**
   * Session attributes/state has changed. Every time a session attribute is put or removed it, this
   * method is executed as notification callback.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void touchSession(@NonNull Context ctx, @NonNull Session session);

  /**
   * Save a session. This method must save:
   *
   * <p>- Session attributes/data - Optionally set Session metadata like: creationTime, lastAccessed
   * time, etc.
   *
   * <p>This method is call after response is send to client, so context and response shouldn't be
   * modified.
   *
   * @param ctx Web context.
   * @param session Current session.
   */
  void saveSession(@NonNull Context ctx, @NonNull Session session);

  /**
   * Renew Session ID. This operation might or might not be implemented by a Session Store.
   *
   * @param ctx Web Context.
   * @param session Session.
   */
  void renewSessionId(@NonNull Context ctx, @NonNull Session session);

  /**
   * Creates a cookie based session and store data in memory.
   *
   * <p>- Session expires after 30 minutes of inactivity. - Session data is not keep after restart.
   *
   * @param cookie Cookie to use.
   * @return Session store.
   */
  static @NonNull SessionStore memory(@NonNull Cookie cookie) {
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
  static @NonNull SessionStore memory(@NonNull Cookie cookie, @NonNull Duration timeout) {
    return memory(SessionToken.cookieId(cookie), timeout);
  }

  /**
   * Creates a session store that save data in memory. - Session expires after 30 minutes of
   * inactivity. - Session data is not keep after restart.
   *
   * @param token Session token.
   * @return Session store.
   */
  static @NonNull SessionStore memory(@NonNull SessionToken token) {
    return new MemorySessionStore(token, Duration.ofMinutes(DEFAULT_TIMEOUT));
  }

  /**
   * Creates a session store that save data in memory. Session data is not keep after restart.
   *
   * @param token Session token.
   * @param timeout Expires session after amount of inactivity time.
   * @return Session store.
   */
  static @NonNull SessionStore memory(@NonNull SessionToken token, @NonNull Duration timeout) {
    return new MemorySessionStore(token, timeout);
  }

  /**
   * Creates a session store that uses (un)signed data. Session data is signed it using <code>
   * HMAC_SHA256</code>.
   *
   * <p>See {@link Cookie#sign(String, String)} and {@link Cookie#unsign(String, String)}.
   *
   * @param cookie Cookie to use.
   * @param secret Secret token to signed data.
   * @return A browser session store.
   */
  static @NonNull SessionStore signed(@NonNull Cookie cookie, @NonNull String secret) {
    return signed(SessionToken.signedCookie(cookie), secret);
  }

  /**
   * Creates a session store that uses (un)signed data. Session data is signed it using <code>
   * HMAC_SHA256</code>.
   *
   * <p>See {@link Cookie#sign(String, String)} and {@link Cookie#unsign(String, String)}.
   *
   * @param token Session token to use.
   * @param secret Secret token to signed data.
   * @return A browser session store.
   */
  static @NonNull SessionStore signed(@NonNull SessionToken token, @NonNull String secret) {
    SneakyThrows.Function<String, Map<String, String>> decoder =
        value -> {
          String unsign = Cookie.unsign(value, secret);
          if (unsign == null) {
            return null;
          }
          return Cookie.decode(unsign);
        };

    SneakyThrows.Function<Map<String, String>, String> encoder =
        attributes -> Cookie.sign(Cookie.encode(attributes), secret);

    return signed(token, decoder, encoder);
  }

  /**
   * Creates a session store that save data into Cookie. Cookie data is (un)signed it using the
   * given decoder and encoder.
   *
   * @param token Token to use.
   * @param decoder Decoder to use.
   * @param encoder Encoder to use.
   * @return Cookie session store.
   */
  static @NonNull SessionStore signed(
      @NonNull SessionToken token,
      @NonNull Function<String, Map<String, String>> decoder,
      @NonNull Function<Map<String, String>, String> encoder) {
    return new SignedSessionStore(token, decoder, encoder);
  }
}
