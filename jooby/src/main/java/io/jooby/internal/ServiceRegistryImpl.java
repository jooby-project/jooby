/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.inject.Provider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistryImpl implements ServiceRegistry {

  private Map<ServiceKey<?>, Provider<?>> registry = new ConcurrentHashMap<>();

  @NonNull @Override public Set<ServiceKey<?>> keySet() {
    return registry.keySet();
  }

  @NonNull @Override public Set<Map.Entry<ServiceKey<?>, Provider<?>>> entrySet() {
    return registry.entrySet();
  }

  @Nullable @Override public <T> T getOrNull(@NonNull ServiceKey<T> key) {
    Provider provider = registry.get(key);
    if (provider == null) {
      return null;
    }
    return (T) provider.get();
  }

  @Nullable @Override public <T> T put(@NonNull ServiceKey<T> key, T service) {
    return put(key, singleton(service));
  }

  @Nullable @Override public <T> T put(@NonNull ServiceKey<T> key, Provider<T> service) {
    return (T) registry.put(key, service);
  }

  @Nullable @Override public <T> T putIfAbsent(@NonNull ServiceKey<T> type, T service) {
    return putIfAbsent(type, singleton(service));
  }

  @Nullable @Override public <T> T putIfAbsent(@NonNull ServiceKey<T> key, Provider<T> service) {
    return (T) registry.putIfAbsent(key, service);
  }

  private static <T> Provider<T> singleton(T service) {
    return () -> service;
  }
}
