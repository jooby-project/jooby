/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kafka;

import javax.annotation.Nonnull;

import org.apache.kafka.clients.producer.KafkaProducer;

import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Kafka producer module: https://jooby.io/modules/kafka.
 * <p>
 * Usage:
 * </p>
 *
 * <pre>{@code
 * {
 *   install(new KafkaProducerModule());
 *
 *   get("/", ctx -> {
 *     KafkaProducer producer = require(KafkaProducer.class);
 *     // work with producer
 *   });
 * }
 * }</pre>
 *
 * application.conf:
 * <pre>{@code
 *  kafka.producer.bootstrap.servers = "localhost:9092"
 *  kafka.producer.acks = "all"
 *  kafka.producer.retries = 0
 *  kafka.producer.key.serializer = "org.apache.kafka.common.serialization.StringSerializer"
 *  kafka.producer.value.serializer = "org.apache.kafka.common.serialization.StringSerializer"
 * }</pre>
 *
 * @author edgar
 * @since 2.9.3
 */
public class KafkaProducerModule implements Extension {
  private final String key;

  /**
   * Creates a kafka producer.
   *
   * @param key Kafka key.
   */
  public KafkaProducerModule(@Nonnull String key) {
    this.key = key;
  }

  /**
   *  Creates a kafka producer. Uses the default key: <code>kafka.producer</code>.
   */
  public KafkaProducerModule() {
    this("kafka.producer");
  }

  @Override public void install(@Nonnull Jooby application) {
    KafkaHelper.install(application, key, KafkaProducer::new);
  }
}
