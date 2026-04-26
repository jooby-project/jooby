/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.exception.RegistryException;
import jakarta.inject.Provider;

public class ServiceRegistryTest {

  /** Simple concrete implementation of ServiceRegistry for testing default methods. */
  private static class TestRegistry implements ServiceRegistry {
    private final Map<ServiceKey<?>, Provider<?>> storage = new HashMap<>();

    @Override
    public Set<ServiceKey<?>> keySet() {
      return storage.keySet();
    }

    @Override
    public Set<Map.Entry<ServiceKey<?>, Provider<?>>> entrySet() {
      return storage.entrySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOrNull(ServiceKey<T> key) {
      Provider<T> provider = (Provider<T>) storage.get(key);
      return provider != null ? provider.get() : null;
    }

    @Override
    public <T> T put(ServiceKey<T> key, Provider<T> service) {
      storage.put(key, service);
      return null; // Simplified
    }

    @Override
    public <T> T put(ServiceKey<T> key, T service) {
      return put(key, (Provider<T>) () -> service);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T putIfAbsent(ServiceKey<T> key, Provider<T> service) {
      return (T) storage.putIfAbsent(key, service);
    }

    @Override
    public <T> T putIfAbsent(ServiceKey<T> key, T service) {
      return putIfAbsent(key, (Provider<T>) () -> service);
    }
  }

  private TestRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new TestRegistry();
  }

  @Test
  void testMapBinder() {
    ServiceRegistry.MapBinder<String, Integer> binder = registry.mapOf(String.class, Integer.class);
    binder.put("one", 1);
    binder.put("two", () -> 2);

    Map<String, Integer> map = binder.get();
    assertEquals(1, map.get("one"));
    assertEquals(2, map.get("two"));
    assertThrows(UnsupportedOperationException.class, () -> map.put("three", 3)); // Unmodifiable
  }

  @Test
  void testMultiBinderList() {
    ServiceRegistry.MultiBinder<String> binder = registry.listOf(String.class);
    binder.add("a").add(() -> "b");

    List<String> list = (List<String>) binder.get();
    assertEquals(List.of("a", "b"), list);

    // Test Reified variant
    ServiceRegistry.MultiBinder<String> reifiedBinder = registry.listOf(Reified.get(String.class));
    reifiedBinder.add("c");
    assertTrue(reifiedBinder.get().contains("c"));
  }

  @Test
  void testMultiBinderSet() {
    ServiceRegistry.MultiBinder<String> binder = registry.setOf(String.class);
    binder.add("a").add("a"); // Duplicate

    Set<String> set = (Set<String>) binder.get();
    assertEquals(1, set.size());

    // Test Reified variant
    ServiceRegistry.MultiBinder<String> reifiedBinder = registry.setOf(Reified.get(String.class));
    assertNotNull(reifiedBinder);
  }

  @Test
  void testGetVariants() {
    registry.put(String.class, "hello");

    assertEquals("hello", registry.get(String.class));
    assertEquals("hello", registry.get(Reified.get(String.class)));
    assertEquals("hello", registry.require(String.class));
    assertEquals("hello", registry.require(Reified.get(String.class)));

    registry.put(ServiceKey.key(String.class, "named"), "world");
    assertEquals("world", registry.require(String.class, "named"));
    assertEquals("world", registry.require(Reified.get(String.class), "named"));
  }

  @Test
  void testGetNotFound() {
    assertThrows(RegistryException.class, () -> registry.get(String.class));
    assertNull(registry.getOrNull(String.class));
    assertNull(registry.getOrNull(Reified.get(String.class)));
  }

  @Test
  void testPutIfAbsentVariants() {
    registry.putIfAbsent(String.class, "first");
    registry.putIfAbsent(String.class, "second");
    assertEquals("first", registry.get(String.class));

    registry.putIfAbsent(Integer.class, (Provider<Integer>) () -> 10);
    assertEquals(10, registry.get(Integer.class));

    registry.putIfAbsent(Reified.get(Long.class), 100L);
    assertEquals(100L, registry.get(Long.class));

    registry.putIfAbsent(Double.class, (Provider<Double>) () -> 1.1);
    assertEquals(1.1, registry.get(Double.class));
  }

  @Test
  void testMultiBinderTypeMismatch() {
    // Register a raw String where a MapBinder is expected
    registry.put(
        ServiceKey.key(Reified.map(String.class, String.class)),
        new Provider<Map<Object, Object>>() {
          @Override
          public Map<Object, Object> get() {
            return Map.of("key", "value");
          }
        });

    assertThrows(RegistryException.class, () -> registry.mapOf(String.class, String.class));
  }

  @Test
  void testMapOfWithReified() {
    ServiceRegistry.MapBinder<String, List<String>> binder =
        registry.mapOf(String.class, new Reified<List<String>>() {});
    assertNotNull(binder);
  }
}
