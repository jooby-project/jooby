/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kafka;

import javax.annotation.Nonnull;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Kafka consumer module: https://jooby.io/modules/kafka.
 * <p>
 * Usage:
 * </p>
 *
 * <pre>{@code
 * {
 *   install(new KafkaConsumerModule());
 *
 *   get("/", ctx -> {
 *     KafkaConsumer producer = require(KafkaConsumer.class);
 *     // work with consumer
 *   });
 * }
 * }</pre>
 *
 * application.conf:
 *  Creates a new kafka consumer module using the <code>kafka.consumer</code> property key.
 * This key must be present in the application configuration file, like:
 *
 * <pre>{@code
 *  kafka.consumer.bootstrap.servers = "localhost:9092"
 *  kafka.consumer.group.id = "group A"
 *  kafka.consumer.key.deserializer = "org.apache.kafka.common.serialization.StringDeserializer"
 *  kafka.consumer.value.deserializer = "org.apache.kafka.common.serialization.StringDeserializer"
 * }</pre>
 *
 * @author edgar
 * @since 2.9.3
 */
public class KafkaConsumerModule implements Extension {
  private final String key;

  /**
   * Creates a new kafka consumer module.
   *
   * @param key Kafka key.
   */
  public KafkaConsumerModule(@Nonnull String key) {
    this.key = key;
  }

  /**
   * Creates a new kafka consumer module. Uses the default key: <code>kafka.consumer</code>.
   */
  public KafkaConsumerModule() {
    this("kafka.consumer");
  }

  @Override public void install(@Nonnull Jooby application) {
    KafkaHelper.install(application, key, KafkaConsumer::new);
  }
}
