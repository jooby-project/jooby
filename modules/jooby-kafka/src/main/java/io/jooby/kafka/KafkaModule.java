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
  String producerKey;
  String consumerKey;
  Properties producerProps;
  Properties consumerProps;

  /**
   * Creates a new kafka producer module using the <code>kafka.producer</code> property key.
   * This key must be present in the application configuration file, like:
   *
   * <pre>{@code
   *  kafka.producer.bootstrap.servers = "localhost:9092"
   *  kafka.producer.acks = "all"
   *  kafka.producer.retries = 0
   *  kafka.producer.key.serializer = "org.apache.kafka.common.serialization.StringSerializer"
   *  kafka.producer.value.serializer = "org.apache.kafka.common.serialization.StringSerializer"
   * }</pre>
   *
   * Creates a new kafka consumer module using the <code>kafka.consumer</code> property key.
   * This key must be present in the application configuration file, like:
   *
   * <pre>{@code
   *  kafka.consumer.bootstrap.servers = "localhost:9092"
   *  kafka.consumer.group.id = "group A"
   *  kafka.consumer.key.deserializer = "org.apache.kafka.common.serialization.StringDeserializer"
   *  kafka.consumer.value.deserializer = "org.apache.kafka.common.serialization.StringDeserializer"
   * }</pre>
   */
  public KafkaModule() {
    this("kafka.producer", "kafka.consumer");
  }

  /**
   * Creates a new kafka producer module. The producer parameter can be one of:
   *
   * - A property key defined in your application configuration file, like <code>producerKey</code>.
   *
   * @param producerKey Database key
   *
   * Creates a new kafka consumer module. The consumer parameter can be one of:
   *
   * - A property key defined in your application configuration file, like <code>consumerKey</code>.
   *
   * @param consumerKey Database key
   */
  public KafkaModule(@Nonnull String producerKey, @Nonnull String consumerKey) {
    this.producerKey = producerKey;
    this.consumerKey = consumerKey;
  }

  /**
   * Creates a new kafka module.
   *
   * @param producerProps kafka producer properties.
   * @param consumerProps kafka consumer properties.
   */
  public KafkaModule(@Nonnull Properties producerProps, @Nonnull Properties consumerProps) {
    this("kafka.producer", "kafka.consumer");
    this.producerProps = producerProps;
    this.consumerProps = consumerProps;
  }


  @Override
  public void install(@Nonnull Jooby application) {
    Config config = application.getConfig();

    if (this.producerProps == null) {
      this.producerProps = properties(config, this.producerKey);
    }

    if (this.consumerProps == null) {
      this.consumerProps = properties(config, this.consumerKey);
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
