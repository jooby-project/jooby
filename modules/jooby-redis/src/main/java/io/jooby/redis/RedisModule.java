/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redis;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;

/**
 * Redis module: https://jooby.io/modules/redis.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *   install(new RedisModule());
 *
 *   get("/", ctx -> {
 *     StatefulRedisConnection redis = require(StatefulRedisConnection.class);
 *     // work with redis
 *   });
 * }
 * }</pre>
 *
 * application.conf:
 *
 * <pre>
 *   redis = "redis://localhost:6379"
 * </pre>
 *
 * Module is built on top of <a href="https://lettuce.io/">lettuce</a>. Once installed you are able
 * to work with:
 *
 * <ul>
 *   <li>io.lettuce.core.RedisClient</li>
 *   <li>io.lettuce.core.api.StatefulRedisConnection</li>
 *   <li>io.lettuce.core.pubsub.StatefulRedisPubSubConnection</li>
 * </ul>
 *
 * Alternative you can pass a redis URI:
 * <pre>
 *   install(new RedisModule("redis://localhost:6379"));
 * </pre>
 *
 * @author edgar
 * @since 2.8.5
 */
public class RedisModule implements Extension {
  private String name;
  private RedisURI uri;

  /**
   * Creates a new redis module. Value must be:
   * - Valid redis URI; or
   * - Property name
   *
   * @param value Redis URI or property name.
   */
  public RedisModule(@NonNull String value) {
    try {
      uri = RedisURI.create(value);
      name = "redis";
    } catch (IllegalArgumentException x) {
      name = value;
    }
  }

  /**
   * Create a new redis module. The application configuration file must have a redis property, like:
   * <pre>
   *   redis = "redis://localhost:6379"
   * </pre>
   */
  public RedisModule() {
    this("redis");
  }

  @Override public void install(@NonNull Jooby application) throws Exception {
    if (uri == null) {
      Config config = application.getConfig();
      uri = Stream.of(name + ".uri", name)
          .filter(config::hasPath)
          .map(config::getString)
          .map(RedisURI::create)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              "Redis uri missing from application configuration: " + name));
    }
    RedisClient client = RedisClient.create(uri);
    StatefulRedisConnection<String, String> connection = client.connect();
    StatefulRedisPubSubConnection<String, String> connectPubSub = client.connectPubSub();

    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
        .createGenericObjectPool(() -> client.connect(), poolConfig);

    // Close client and connection on shutdown
    application.onStop(pool::close);
    application.onStop(connection::close);
    application.onStop(connectPubSub::close);
    application.onStop(client::shutdown);

    ServiceRegistry registry = application.getServices();

    registry.putIfAbsent(ServiceKey.key(RedisClient.class), client);
    registry.put(ServiceKey.key(RedisClient.class, name), client);

    registry.putIfAbsent(ServiceKey.key(StatefulRedisConnection.class), connection);
    registry.put(ServiceKey.key(StatefulRedisConnection.class, name), connection);

    registry.putIfAbsent(ServiceKey.key(GenericObjectPool.class), pool);
    registry.put(ServiceKey.key(GenericObjectPool.class, name), pool);

    registry.putIfAbsent(ServiceKey.key(StatefulRedisPubSubConnection.class), connectPubSub);
    registry.put(ServiceKey.key(StatefulRedisPubSubConnection.class, name), connectPubSub);
  }
}
