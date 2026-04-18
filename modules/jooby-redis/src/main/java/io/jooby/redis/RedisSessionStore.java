/*
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

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
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

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final SessionToken token;
  private String namespace = "sessions";
  private Duration timeout = Duration.ofMinutes(DEFAULT_TIMEOUT);
  private final GenericObjectPool<StatefulRedisConnection<String, String>> pool;

  /**
   * Creates a new session store.
   *
   * @param token Session token.
   * @param pool Redis connection pool.
   */
  public RedisSessionStore(
      SessionToken token, GenericObjectPool<StatefulRedisConnection<String, String>> pool) {
    this.token = token;
    this.pool = pool;
  }

  /**
   * Creates a new session store.
   *
   * @param token Session token.
   * @param redis Redis connection.
   */
  public RedisSessionStore(SessionToken token, RedisClient redis) {
    this(
        token,
        ConnectionPoolSupport.createGenericObjectPool(
            redis::connect, new GenericObjectPoolConfig<>()));
  }

  /**
   * Redis namespace (key prefix).
   *
   * @return Redis namespace (key prefix). Default is: <code>sessions</code>.
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Set redis namespace or key prefix.
   *
   * @param namespace Redis namespace or key prefix.
   * @return This store.
   */
  public RedisSessionStore setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Session timeout.
   *
   * @return Session timeout. Default is: <code>30 minutes</code>.
   */
  public @Nullable Duration getTimeout() {
    return timeout;
  }

  /**
   * Set/change session timeout.
   *
   * @param timeout Timeout must be positive value. Otherwise, timeout is disabled.
   * @return This store.
   */
  public RedisSessionStore setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Remove session timeout.
   *
   * @return This store.
   */
  public RedisSessionStore noTimeout() {
    this.timeout = null;
    return this;
  }

  /**
   * Session token.
   *
   * @return Session token.
   */
  public SessionToken getToken() {
    return token;
  }

  @Override
  public Session newSession(Context ctx) {
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

  @Nullable @Override
  public Session findSession(Context ctx) {
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
    return withConnection(
        connection -> {
          RedisCommands<String, String> commands = connection.sync();
          String redisId = key(sessionId);
          Map<String, String> data = commands.hgetall(redisId);
          if (data == null || data.isEmpty()) {
            return null;
          }
          Optional.ofNullable(timeout)
              .map(Duration::getSeconds)
              .ifPresent(seconds -> commands.expire(redisId, seconds));
          Instant lastAccessedTime = Instant.parse(data.remove(LAST_ACCESSED_AT));
          Instant createdAt = Instant.parse(data.remove(CREATED_AT));

          token.saveToken(ctx, sessionId);

          return Session.create(ctx, sessionId, new ConcurrentHashMap<>(data))
              .setCreationTime(createdAt)
              .setLastAccessedTime(lastAccessedTime);
        });
  }

  @Override
  public void deleteSession(Context ctx, Session session) {
    String sessionId = session.getId();

    withConnection(connection -> connection.async().del(key(sessionId)));

    token.deleteToken(ctx, sessionId);
  }

  @Override
  public void touchSession(Context ctx, Session session) {
    saveSession(ctx, session);

    token.saveToken(ctx, session.getId());
  }

  @Override
  public void saveSession(Context ctx, Session session) {
    saveSession(session.getId(), new HashMap<>(session.toMap()));
  }

  @Override
  public void renewSessionId(Context ctx, Session session) {}

  private void saveSession(String sessionId, Map<String, String> data) {
    withConnection(
        connection -> {
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
          return commands
              .exec()
              .handle(
                  (value, cause) -> {
                    if (cause != null) {
                      log.error("unable to create session: {}", sessionId, cause);
                      return sessionId;
                    } else {
                      Optional.ofNullable(timeout)
                          .map(Duration::getSeconds)
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
