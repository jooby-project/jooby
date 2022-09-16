/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redis;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;
import io.jooby.SneakyThrows;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;

/**
 * Redis session store.
 *
 * @author edgar
 * @since 2.8.5
 */
public class RedisSessionStore implements SessionStore {

  private static final String LAST_ACCESSED_AT = "__accessed_at";
  private static final String CREATED_AT = "__created_at";

  private Logger log = LoggerFactory.getLogger(getClass());

  private SessionToken token = SessionToken.cookieId(SessionToken.SID);
  private String namespace = "sessions";
  private Duration timeout = Duration.ofMinutes(DEFAULT_TIMEOUT);
  private GenericObjectPool<StatefulRedisConnection<String, String>> pool;

  /**
   * Creates a new session store.
   *
   * @param pool Redis connection pool.
   */
  public RedisSessionStore(
      @NonNull GenericObjectPool<StatefulRedisConnection<String, String>> pool) {
    this.pool = pool;
  }

  /**
   * Creates a new session store.
   *
   * @param redis Redis connection.
   */
  public RedisSessionStore(@NonNull RedisClient redis) {
    this(ConnectionPoolSupport
        .createGenericObjectPool(() -> redis.connect(), new GenericObjectPoolConfig()));
  }

  /**
   * Redis namespace (key prefix).
   *
   * @return Redis namespace (key prefix). Default is: <code>sessions</code>.
   */
  public @NonNull String getNamespace() {
    return namespace;
  }

  /**
   * Set redis namespace or key prefix.
   *
   * @param namespace Redis namespace or key prefix.
   * @return This store.
   */
  public @NonNull RedisSessionStore setNamespace(@NonNull String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Session timeout.
   *
   * @return Session timeout. Default is: <code>30 minutes</code>.
   */
  public @NonNull Duration getTimeout() {
    return timeout;
  }

  /**
   * Set/change session timeout.
   *
   * @param timeout Timeout must be positive value. Otherwise, timeout is disabled.
   * @return This store.
   */
  public @NonNull RedisSessionStore setTimeout(@NonNull Duration timeout) {
    this.timeout = Optional.ofNullable(timeout)
        .filter(t -> t.getSeconds() > 0)
        .orElse(null);
    return this;
  }

  /**
   * Remove session timeout.
   *
   * @return This store.
   */
  public @NonNull RedisSessionStore noTimeout() {
    this.timeout = null;
    return this;
  }

  /**
   * Session token.
   *
   * @return Session token. Uses a cookie by default: {@link SessionToken#SID}.
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
  public @NonNull RedisSessionStore setToken(@NonNull SessionToken token) {
    this.token = token;
    return this;
  }

  @NonNull @Override public Session newSession(@NonNull Context ctx) {
    String sessionId = token.newToken();

    Instant now = Instant.now();
    String isoNow = DateTimeFormatter.ISO_INSTANT.format(now);

    Map<String, String> data = new HashMap<>();
    data.put(LAST_ACCESSED_AT, isoNow);
    data.put(CREATED_AT, isoNow);

    saveSession(sessionId, data);

    token.saveToken(ctx, sessionId);

    return Session.create(ctx, sessionId, new ConcurrentHashMap<>())
        .setLastAccessedTime(now)
        .setCreationTime(now);
  }

  @Nullable @Override public Session findSession(@NonNull Context ctx) {
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
    return withConnection(connection -> {
      RedisCommands<String, String> commands = connection.sync();
      String redisId = key(sessionId);
      Map<String, String> data = commands.hgetall(redisId);
      if (data == null || data.isEmpty()) {
        return null;
      }
      Optional.ofNullable(timeout).map(Duration::getSeconds)
          .ifPresent(seconds -> commands.expire(redisId, seconds));
      Instant lastAccessedTime = Instant.parse(data.remove(LAST_ACCESSED_AT));
      Instant createdAt = Instant.parse(data.remove(CREATED_AT));

      token.saveToken(ctx, sessionId);

      return Session.create(ctx, sessionId, new ConcurrentHashMap<>(data))
          .setCreationTime(createdAt)
          .setLastAccessedTime(lastAccessedTime);
    });
  }

  @Override public void deleteSession(@NonNull Context ctx, @NonNull Session session) {
    String sessionId = session.getId();

    withConnection(connection -> connection.async().del(key(sessionId)));

    token.deleteToken(ctx, sessionId);
  }

  @Override public void touchSession(@NonNull Context ctx, @NonNull Session session) {
    saveSession(ctx, session);

    token.saveToken(ctx, session.getId());
  }

  @Override public void saveSession(@NonNull Context ctx, @NonNull Session session) {
    saveSession(session.getId(), new HashMap<>(session.toMap()));
  }

  @Override public void renewSessionId(@NonNull Context ctx, @NonNull Session session) {

  }

  private void saveSession(String sessionId, Map<String, String> data) {
    withConnection(connection -> {

      Instant now = Instant.now();
      String isoNow = DateTimeFormatter.ISO_INSTANT.format(now);

      data.put(LAST_ACCESSED_AT, isoNow);
      data.put(CREATED_AT, isoNow);

      RedisAsyncCommands<String, String> commands = connection.async();
      String redisId = key(sessionId);
      // start transaction
      commands.multi();
      // delete existing
      commands.del(redisId);
      // save again
      commands.hset(redisId, data);
      // commit
      return commands.exec().handle((value, cause) -> {
        if (cause != null) {
          log.error("unable to create session: {}", sessionId, cause);
          return sessionId;
        } else {
          Optional.ofNullable(timeout).map(Duration::getSeconds)
              .ifPresent(seconds -> commands.expire(redisId, seconds));
          return value;
        }
      });
    });
  }

  private <T> T withConnection(SneakyThrows.Function<StatefulRedisConnection, T> callback) {
    try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {
      return callback.apply(connection);
    } catch (Exception cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  private String key(String id) {
    return namespace + ":" + id;
  }
}
