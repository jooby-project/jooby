/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kafka;

import javax.annotation.Nonnull;

import io.jooby.Extension;
import io.jooby.Jooby;

/**
 * Kafka module: https://jooby.io/modules/kafka.
 * <p>
 * Usage:
 * </p>
 *
 * <pre>{@code
 * {
 *   install(new KafkaModule());
 *
 *   get("/", ctx -> {
 *     KafkaConsumer producer = require(KafkaConsumer.class);
 *     // work with consumer
 *
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
 * Creates a new kafka consumer module using the <code>kafka.consumer</code> property key.
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
public class KafkaModule implements Extension {
  private final String producerKey;

  private final String consumerKey;

  /**
   * Creates a new kafka producer module using the <code>kafka.producer</code> and
   *  <code>kafka.consumer</code> property keys.
   */
  public KafkaModule() {
    this("kafka.producer", "kafka.consumer");
  }

  /**
   * Creates a new kafka producer module.
   *
   * @param producerKey Database key
   * @param consumerKey Database key
   */
  public KafkaModule(@Nonnull String producerKey, @Nonnull String consumerKey) {
    this.producerKey = producerKey;
    this.consumerKey = consumerKey;
  }

  @Override
  public void install(@Nonnull Jooby application) {
    new KafkaConsumerModule(consumerKey).install(application);

    new KafkaProducerModule(producerKey).install(application);
  }
}
