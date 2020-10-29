/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kafka;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import javax.annotation.Nonnull;
import java.util.Properties;

/**
 * Redis module: https://jooby.io/modules/redis.
 * <p>
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
 * <p>
 * application.conf:
 *
 * <pre>
 *   redis = "redis://localhost:6379"
 * </pre>
 * <p>
 * Module is built on top of <a href="https://lettuce.io/">lettuce</a>. Once installed you are able
 * to work with:
 *
 * <ul>
 *   <li>io.lettuce.core.RedisClient</li>
 *   <li>io.lettuce.core.api.StatefulRedisConnection</li>
 *   <li>io.lettuce.core.pubsub.StatefulRedisPubSubConnection</li>
 * </ul>
 * <p>
 * Alternative you can pass a redis URI:
 * <pre>
 *   install(new RedisModule("redis://localhost:6379"));
 * </pre>
 *
 * @author edgar
 * @since 2.8.5
 */
public class KafkaModule implements Extension {
  Properties producerProps;
  Properties consumerProps;

  /**
   * Creates a new kafka module. Value must be:
   * - Valid redis URI; or
   * - Property name
   *
   * @param producerProps kafka producer properties.
   * @param consumerProps kafka consumer properties.
   */
  public KafkaModule(@Nonnull Properties producerProps, @Nonnull Properties consumerProps) {
    this.producerProps = producerProps;
    this.consumerProps = consumerProps;
  }

  /**
   * Create a new redis module. The application configuration file must have a redis property, like:
   * <pre>
   *   redis = "redis://localhost:6379"
   * </pre>
   */
  public KafkaModule() {
  }

  @Override
  public void install(@Nonnull Jooby application) {
    Config config = application.getConfig();

    if (this.producerProps == null) {
      this.producerProps = properties(config, "kafka.producer");
    }

    if (this.consumerProps == null) {
      this.consumerProps = properties(config, "kafka.consumer");
    }

    ServiceRegistry registry = application.getServices();

    if (this.producerProps != null) {
      KafkaProducer producer = new KafkaProducer(producerProps);
      application.onStop(producer::close);
      registry.putIfAbsent(KafkaProducer.class, producer);
    }

    if (this.consumerProps != null) {
      KafkaConsumer consumer = new KafkaConsumer(consumerProps);
      application.onStop(consumer::close);
      registry.putIfAbsent(KafkaConsumer.class, consumer);
    }
  }

  private static Properties properties(final Config config, final String key) {
    Properties props = new Properties();
    if (config.hasPath(key)) {
      // dump
      config.getConfig(key).entrySet().forEach(
          e -> props.setProperty(e.getKey(), e.getValue().unwrapped().toString()));
    }
    return props;
  }
}
