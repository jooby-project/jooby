/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistryImpl implements ServiceRegistry {

  private Map<ServiceKey<?>, Object> registry = new ConcurrentHashMap<>();

  @Nonnull @Override public Set<ServiceKey<?>> keySet() {
    return registry.keySet();
  }

  @Nullable @Override public <T> T getOrNull(@Nonnull ServiceKey<T> key) {
    return (T) registry.get(key);
  }

  @Nullable @Override public <T> T put(@Nonnull ServiceKey<T> key, T service) {
    return (T) registry.put(key, service);
  }

  @Nullable @Override public <T> T putIfAbsent(@Nonnull ServiceKey<T> type, T service) {
    return (T) registry.putIfAbsent(type, service);
  }
}
