/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kafka;

import java.util.Properties;
import java.util.function.Function;

import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

class KafkaHelper {

  public static void install(Jooby application, String key,
      Function<Properties, AutoCloseable> factory) {
    Environment environment = application.getEnvironment();

    Properties properties = new Properties();
    properties.putAll(environment.getProperties(key, null));

    AutoCloseable service = factory.apply(properties);
    ServiceRegistry registry = application.getServices();

    application.onStop(service::close);
    Class serviceType = service.getClass();
    registry.putIfAbsent(serviceType, service);
    registry.put(ServiceKey.key(serviceType, key), service);
  }
}
