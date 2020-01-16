/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.aws;

import com.amazonaws.AmazonWebServiceClient;
import io.jooby.SneakyThrows;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

public class ServiceShutdown implements AutoCloseable {
  private Object service;

  private Logger log;

  public ServiceShutdown(Logger log, Object service) {
    this.log = log;
    this.service = service;
  }

  @Override public void close() throws Exception {
    if (service instanceof AmazonWebServiceClient) {
      log.debug("Stopping {}", ((AmazonWebServiceClient) service).getServiceName());
      ((AmazonWebServiceClient) service).shutdown();
    } else {
      // reflection based
      try {
        log.debug("Stopping {}", service.getClass().getSimpleName());
        Method shutdown = Stream.of(shutdownMethod("shutdown"), shutdownMethod("shutdownNow"))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (shutdown == null) {
          log.info("Unable to stop {}", service.getClass().getSimpleName());
        } else {
          shutdown.invoke(service);
        }
      } catch (InvocationTargetException x) {
        throw SneakyThrows.propagate(x.getCause());
      }
    }
  }

  private Method shutdownMethod(String name) {
    try {
      return service.getClass().getDeclaredMethod(name);
    } catch (NoSuchMethodException x) {
      return null;
    }
  }
}
