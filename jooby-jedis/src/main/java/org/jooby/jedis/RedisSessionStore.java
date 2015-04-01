/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.jedis;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jooby.Session;
import org.jooby.Session.Builder;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * A {@link Session.Store} powered by <a href="http://redis.io/">Redis</a>.
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Redis());
 *
 *   session(RedisSessionStore.class);
 *
 *   get("/", req {@literal ->} {
 *    req.session().set("name", "jooby");
 *   });
 * }
 * </pre>
 *
 * The <code>name</code> attribute and value will be stored in a
 * <a href="http://redis.io/">Redis</a> db.
 *
 * Session are persisted as
 * a <a href="http://redis.io/topics/data-types#hashes">Redis Hash</a>.
 *
 * <h2>options</h2>
 *
 * <h3>timeout</h3>
 * <p>
 * By default, a redis session will expire after <code>30 minutes</code>. Changing the default
 * timeout is as simple as:
 * </p>
 *
 * <pre>
 * # 8 hours
 * session.timeout = 8h
 *
 * # 15 seconds
 * session.timeout = 15
 *
 * # 120 minutes
 * session.timeout = 120m
 * </pre>
 *
 * If no timeout is required, use <code>-1</code>.
 *
 * <h3>key prefix</h3>
 * <p>
 * Default redis key prefix is <code>sessions</code>. Sessions in redis will look like:
 * <code>sessions:ID</code>
 *
 * <p>
 * It's possible to change the default key setting the <code>jedis.sesssion.prefix</code> properties
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
@Singleton
public class RedisSessionStore implements Session.Store {

  private JedisPool pool;

  private int timeout;

  private String prefix;

  /**
   * Creates a new {@link RedisSessionStore}.
   *
   * @param pool Jedis pool.
   * @param prefix Session key prefix on redis.
   * @param timeout Session timeout in seconds.
   */
  public RedisSessionStore(final JedisPool pool, final String prefix,
      final int timeout) {
    this.pool = requireNonNull(pool, "Jedis pool is required.");
    this.timeout = timeout;
    this.prefix = requireNonNull(prefix, "Prefix is required.");
  }

  /**
   * Creates a new {@link RedisSessionStore}.
   *
   * @param pool Jedis pool.
   * @param prefix Session key prefix on redis.
   * @param timeout Session timeout expression, like <code>30m</code>.
   */
  @Inject
  public RedisSessionStore(final JedisPool pool,
      final @Named("jedis.session.prefix") String prefix,
      @Named("jedis.session.timeout") final String timeout) {
    this(pool, prefix, seconds(timeout));
  }

  @Override
  public Session get(final Builder builder) {
    try (Jedis jedis = pool.getResource()) {
      String key = key(builder.sessionId());
      Map<String, String> attrs = jedis.hgetAll(key);
      if (timeout > 0) {
        // touch session
        jedis.expire(key, timeout);
      }
      return builder
          .accessedAt(Long.parseLong(attrs.remove("_accessedAt")))
          .createdAt(Long.parseLong(attrs.remove("_createdAt")))
          .savedAt(Long.parseLong(attrs.remove("_savedAt")))
          .set(attrs)
          .build();
    }
  }

  @Override
  public void save(final Session session) {
    try (Jedis jedis = pool.getResource()) {
      String key = key(session);
      Map<String, String> attrs = new HashMap<>(session.attributes());
      attrs.put("_createdAt", Long.toString(session.createdAt()));
      attrs.put("_accessedAt", Long.toString(session.accessedAt()));
      attrs.put("_savedAt", Long.toString(session.savedAt()));
      jedis.hmset(key, attrs);
      if (timeout > 0) {
        jedis.expire(key, timeout);
      }
    }
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    try (Jedis jedis = pool.getResource()) {
      jedis.del(key(id));
    }

  }

  private String key(final String id) {
    return prefix + ":" + id;
  }

  private String key(final Session session) {
    return key(session.id());
  }

  private static int seconds(final String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
          .withValue("timeout", ConfigValueFactory.fromAnyRef(value));
      return (int) config.getDuration("timeout", TimeUnit.SECONDS);
    }
  }
}
