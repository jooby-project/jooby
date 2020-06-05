/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redis;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
  private StatefulRedisConnection<String, String> connection;

  /**
   * Creates a new session store.
   *
   * @param connection Redis connection.
   */
  public RedisSessionStore(@Nonnull StatefulRedisConnection connection) {
    this.connection = connection;
  }

  /**
   * Redis namespace (key prefix).
   *
   * @return Redis namespace (key prefix). Default is: <code>sessions</code>.
   */
  public @Nonnull String getNamespace() {
    return namespace;
  }

  /**
   * Set redis namespace or key prefix.
   *
   * @param namespace Redis namespace or key prefix.
   * @return This store.
   */
  public @Nonnull RedisSessionStore setNamespace(@Nonnull String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Session timeout.
   *
   * @return Session timeout. Default is: <code>30 minutes</code>.
   */
  public @Nonnull Duration getTimeout() {
    return timeout;
  }

  /**
   * Set/change session timeout.
   *
   * @param timeout Timeout must be positive value. Otherwise, timeout is disabled.
   * @return This store.
   */
  public @Nonnull RedisSessionStore setTimeout(@Nonnull Duration timeout) {
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
  public @Nonnull RedisSessionStore noTimeout() {
    this.timeout = null;
    return this;
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
  public @Nonnull RedisSessionStore setToken(@Nonnull SessionToken token) {
    this.token = token;
    return this;
  }

  @Nonnull @Override public Session newSession(@Nonnull Context ctx) {
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

  @Nullable @Override public Session findSession(@Nonnull Context ctx) {
    String sessionId = token.findToken(ctx);
    if (sessionId == null) {
      return null;
    }
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
  }

  @Override public void deleteSession(@Nonnull Context ctx, @Nonnull Session session) {
    String sessionId = session.getId();
    connection.async().del(key(sessionId));

    token.deleteToken(ctx, sessionId);
  }

  @Override public void touchSession(@Nonnull Context ctx, @Nonnull Session session) {
    saveSession(ctx, session);

    token.saveToken(ctx, session.getId());
  }

  @Override public void saveSession(@Nonnull Context ctx, @Nonnull Session session) {
    saveSession(session.getId(), new HashMap<>(session.toMap()));
  }

  @Override public void renewSessionId(@Nonnull Context ctx, @Nonnull Session session) {

  }

  private void saveSession(String sessionId, Map<String, String> data) {
    RedisAsyncCommands<String, String> commands = connection.async();

    Instant now = Instant.now();
    String isoNow = DateTimeFormatter.ISO_INSTANT.format(now);

    data.put(LAST_ACCESSED_AT, isoNow);
    data.put(CREATED_AT, isoNow);

    String redisId = key(sessionId);
    commands.hmset(redisId, data).handle((value, cause) -> {
      if (cause != null) {
        log.error("unable to create redis session: {}", sessionId, cause);
        return sessionId;
      } else {
        Optional.ofNullable(timeout).map(Duration::getSeconds)
            .ifPresent(seconds -> commands.expire(redisId, seconds));
        return value;
      }
    });
  }

  private String key(String id) {
    return namespace + ":" + id;
  }
}
