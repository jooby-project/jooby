/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.exception.RegistryException;
import jakarta.inject.Provider;

/**
 * Default registry that uses a simply key/value mechanism for storing and retrieving services.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface ServiceRegistry extends Registry {

  /**
   * Map binder, allow to partially register map entries.
   *
   * @param <K> Map key.
   * @param <V> Map value.
   */
  class MapBinder<K, V> implements Provider<Map<K, V>> {
    private final Map<K, Provider<V>> services;

    private MapBinder() {
      this.services = new HashMap<>();
    }

    /**
     * Put a service into a map.
     *
     * @param key Key.
     * @param service Service value.
     * @return This binder.
     */
    public MapBinder<K, V> put(K key, V service) {
      return put(key, () -> service);
    }

    /**
     * Put a service into a map.
     *
     * @param key Key.
     * @param service Service value.
     * @return This binder.
     */
    public MapBinder<K, V> put(K key, Provider<V> service) {
      services.put(key, service);
      return this;
    }

    @Override
    public Map<K, V> get() {
      return services.entrySet().stream()
          .collect(toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
  }

  /**
   * List/Set binder, allow to partially register service and fetch them all as list/set.
   *
   * @param <T> Item type.
   */
  abstract class MultiBinder<T> implements Provider<Collection<T>> {
    protected final Collection<Provider<T>> services;

    private MultiBinder(Collection<Provider<T>> services) {
      this.services = services;
    }

    /**
     * Add a service to final list.
     *
     * @param service Service to add.
     * @return This binder.
     */
    public MultiBinder<T> add(T service) {
      return add(() -> service);
    }

    /**
     * Add a service to final list.
     *
     * @param service Service to add.
     * @return This binder.
     */
    public MultiBinder<T> add(Provider<T> service) {
      services.add(service);
      return this;
    }

    static <T> MultiBinder<T> list() {
      return new MultiBinder<T>(new ArrayList<>()) {
        @SuppressWarnings("unchecked")
        @Override
        public List<T> get() {
          return (List<T>) List.of(services.stream().map(Provider::get).toArray());
        }
      };
    }

    static <T> MultiBinder<T> set() {
      return new MultiBinder<T>(new HashSet<>()) {
        @SuppressWarnings("unchecked")
        @Override
        public Set<T> get() {
          return (Set<T>) Set.of(services.stream().map(Provider::get).toArray());
        }
      };
    }
  }

  /**
   * Registered service keys.
   *
   * @return Service keys.
   */
  Set<ServiceKey<?>> keySet();

  /**
   * Registered service entries.
   *
   * @return Service entries.
   */
  Set<Map.Entry<ServiceKey<?>, Provider<?>>> entrySet();

  /**
   * Retrieve a service/resource by key.
   *
   * @param key Service/resource key.
   * @param <T> Service/resource type.
   * @return Service.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  default <T> T get(@NonNull ServiceKey<T> key) {
    T service = getOrNull(key);
    if (service == null) {
      throw new RegistryException("Service not found: " + key);
    }
    return service;
  }

  /**
   * Retrieve a service/resource by key.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  default <T> T get(@NonNull Class<T> type) {
    return get(ServiceKey.key(type));
  }

  /**
   * Retrieve a service/resource by key.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  default <T> T get(@NonNull Reified<T> type) {
    return get(ServiceKey.key(type));
  }

  /**
   * Retrieve an existing service or <code>null</code> if not exists.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service or <code>null</code>.
   */
  default @Nullable <T> T getOrNull(@NonNull Reified<T> type) {
    return getOrNull(ServiceKey.key(type));
  }

  /**
   * Retrieve an existing service or <code>null</code> if not exists.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service or <code>null</code>.
   */
  default @Nullable <T> T getOrNull(@NonNull Class<T> type) {
    return getOrNull(ServiceKey.key(type));
  }

  /**
   * Retrieve an existing service or <code>null</code> if not exists.
   *
   * @param key Service/resource key.
   * @param <T> Service/resource type.
   * @return Service or <code>null</code>.
   */
  @Nullable <T> T getOrNull(@NonNull ServiceKey<T> key);

  /**
   * List binder. You can gradually add service of the same type and retrieve them all as list.
   *
   * @param type Type.
   * @return A new list binder.
   * @param <T> Service type.
   */
  default <T> MultiBinder<T> listOf(@NonNull Class<T> type) {
    return multiBinder(Reified.list(type), MultiBinder.list());
  }

  /**
   * List binder. You can gradually add service of the same type and retrieve them all as list.
   *
   * @param type Type.
   * @return A new list binder.
   * @param <T> Service type.
   */
  default <T> MultiBinder<T> listOf(@NonNull Reified<T> type) {
    return multiBinder(Reified.list(type.getType()), MultiBinder.list());
  }

  /**
   * Set binder. You can gradually add service of the same type and retrieve them all as set.
   *
   * @param type Type.
   * @return A new set binder.
   * @param <T> Service type.
   */
  default <T> MultiBinder<T> setOf(@NonNull Class<T> type) {
    return multiBinder(Reified.set(type), MultiBinder.set());
  }

  /**
   * Set binder. You can gradually add service of the same type and retrieve them all as set.
   *
   * @param type Type.
   * @return A new set binder.
   * @param <T> Service type.
   */
  default <T> MultiBinder<T> setOf(@NonNull Reified<T> type) {
    return multiBinder(Reified.set(type.getType()), MultiBinder.set());
  }

  /**
   * Map binder. You can gradually put service of the same type and retrieve them all as map.
   *
   * @param keyType Key Type.
   * @param valueType Service Type.
   * @return A new map binder.
   * @param <K> Key type.
   * @param <V> Service type.
   */
  default <K, V> MapBinder<K, V> mapOf(@NonNull Class<K> keyType, @NonNull Class<V> valueType) {
    return multiBinder(Reified.map(keyType, valueType), new MapBinder<>());
  }

  /**
   * Map binder. You can gradually put service of the same type and retrieve them all as map.
   *
   * @param keyType Key Type.
   * @param valueType Service Type.
   * @return A new map binder.
   * @param <K> Key type.
   * @param <V> Service type.
   */
  default <K, V> MapBinder<K, V> mapOf(@NonNull Class<K> keyType, @NonNull Reified<V> valueType) {
    return multiBinder(Reified.map(keyType, valueType.getType()), new MapBinder<>());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <P extends Provider> P multiBinder(@NonNull Reified reified, @NonNull P multibinder) {
    ServiceKey<?> key = ServiceKey.key(reified);
    var existing = putIfAbsent(key, multibinder);
    if (existing != null) {
      if (multibinder.getClass().isInstance(existing)) {
        multibinder = (P) existing;
      } else {
        throw new RegistryException("Mismatch type for key: " + reified);
      }
    }
    return multibinder;
  }

  /**
   * Put a service in this registry. This method overrides any previous registered service.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T put(@NonNull Class<T> type, Provider<T> service) {
    return put(ServiceKey.key(type), service);
  }

  /**
   * Put a service in this registry. This method overrides any previous registered service.
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T put(@NonNull ServiceKey<T> key, Provider<T> service);

  /**
   * Put a service in this registry. This method overrides any previous registered service.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T put(@NonNull Class<T> type, T service) {
    return put(ServiceKey.key(type), service);
  }

  /**
   * Put a service in this registry. This method overrides any previous registered service.
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T put(@NonNull ServiceKey<T> key, T service);

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T putIfAbsent(@NonNull ServiceKey<T> key, T service);

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T putIfAbsent(@NonNull Class<T> type, T service) {
    return putIfAbsent(ServiceKey.key(type), service);
  }

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T putIfAbsent(@NonNull Class<T> type, Provider<T> service) {
    return putIfAbsent(ServiceKey.key(type), service);
  }

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T putIfAbsent(@NonNull Reified<T> type, T service) {
    return putIfAbsent(ServiceKey.key(type), service);
  }

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T putIfAbsent(@NonNull Reified<T> type, Provider<T> service) {
    return putIfAbsent(ServiceKey.key(type), service);
  }

  /**
   * If the specified key is not already associated with a service (or is mapped to null) associates
   * it with the given value and returns null, else returns the current value.
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T putIfAbsent(@NonNull ServiceKey<T> key, Provider<T> service);

  default @Override <T> T require(@NonNull Class<T> type) {
    return get(ServiceKey.key(type));
  }

  default @Override <T> T require(@NonNull Class<T> type, @NonNull String name) {
    return get(ServiceKey.key(type, name));
  }

  default @Override <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return get(key);
  }

  @NonNull @Override
  default <T> T require(@NonNull Reified<T> type, @NonNull String name) throws RegistryException {
    return get(ServiceKey.key(type, name));
  }

  @NonNull @Override
  default <T> T require(@NonNull Reified<T> type) throws RegistryException {
    return get(ServiceKey.key(type));
  }
}
