/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redis;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.stream.Stream;

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
  private Object metricRegistry;
  private Duration eventPublishInterval = Duration.ZERO;
  private int ioThreadPoolSize = 0;
  private int computationThreadPoolSize = 0;


  /**
   * Creates a new redis module. Value must be:
   * - Valid redis URI; or
   * - Property name
   *
   * @param value Redis URI or property name.
   */
  public RedisModule(@Nonnull String value) {
    try {
      uri = RedisURI.create(value);
      name = value;
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


  /**
   * Sets a {@code MetricRegistry} from outside caller
   * lettuce metrics is depends on <a href="https://github.com/LatencyUtils/LatencyUtils">LatencyUtils</a>. You should add it to your maven dependencies
   *
   * @param metricsInterval Redis EventPublisher EmitInterval.
   * @param metricRegistry an instance of {@code MetricRegistry}
   * @return this instance
   */
  public RedisModule metricRegistry(@Nonnull Duration metricsInterval, @Nonnull Object metricRegistry) {
    this.eventPublishInterval = metricsInterval;
    this.metricRegistry = metricRegistry;
    return this;
  }

  /**
   * Sets a {@code MetricRegistry} from outside caller
   * Default EventPublisher EmitInterval is 10 MINUTES
   * lettuce metrics is depends on <a href="https://github.com/LatencyUtils/LatencyUtils">LatencyUtils</a>. You should add it to your maven dependencies
   *
   * @param metricRegistry an instance of {@code MetricRegistry}
   * @return this instance
   */
  public RedisModule metricRegistry(@Nonnull Object metricRegistry) {
    this.metricRegistry = metricRegistry;
    return this;
  }

  /**
   * @param ioThreadPoolSize
   * @return this instance
   */
  public RedisModule withIOThreadPoolSize(@Nonnull int ioThreadPoolSize) {
    this.ioThreadPoolSize = ioThreadPoolSize;
    return this;
  }

  /**
   * @param computationThreadPoolSize
   * @return this instance
   */
  public RedisModule withComputationThreadPoolSize(@Nonnull int computationThreadPoolSize) {
    this.computationThreadPoolSize = computationThreadPoolSize;
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
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

    ClientResources.Builder builder = DefaultClientResources.builder();
    if (ioThreadPoolSize>0){
      builder.ioThreadPoolSize(ioThreadPoolSize);
    }
    if (computationThreadPoolSize>0){
      builder.computationThreadPoolSize(computationThreadPoolSize);
    }

    if (metricRegistry != null) {
      if (!this.eventPublishInterval.isZero()) {
        builder.commandLatencyPublisherOptions(()-> this.eventPublishInterval);
      }
    }

    RedisClient client = RedisClient.create(builder.build(), uri);
    StatefulRedisConnection<String, String> connection = client.connect();
    StatefulRedisPubSubConnection<String, String> connectPubSub = client.connectPubSub();

    // Close client and connection on shutdown
    application.onStop(connection::close);
    application.onStop(connectPubSub::close);
    application.onStop(client::shutdown);

    ServiceRegistry registry = application.getServices();

    registry.putIfAbsent(ServiceKey.key(RedisClient.class), client);
    registry.put(ServiceKey.key(RedisClient.class, name), client);

    registry.putIfAbsent(ServiceKey.key(StatefulRedisConnection.class), connection);
    registry.put(ServiceKey.key(StatefulRedisConnection.class, name), connection);

    registry.putIfAbsent(ServiceKey.key(StatefulRedisPubSubConnection.class), connectPubSub);
    registry.put(ServiceKey.key(StatefulRedisPubSubConnection.class, name), connectPubSub);

    if(metricRegistry != null) {
      EventBus eventBus = connection.getResources().eventBus();
      eventBus.get().subscribe(new RedisEventConsumer(metricRegistry, name));
    }
  }

}
