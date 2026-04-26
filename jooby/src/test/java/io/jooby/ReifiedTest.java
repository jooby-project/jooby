/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ReifiedTest {

  @Test
  public void anonymousSubclass() {
    // Standard use case: anonymous subclass to capture generic type
    Reified<List<String>> listStr = new Reified<List<String>>() {};

    assertEquals(List.class, listStr.getRawType());
    assertTrue(listStr.getType().toString().contains("java.util.List<java.lang.String>"));
  }

  @Test
  public void runtimeExceptionOnMissingTypeParameter() {
    // This should fail because it's not a parameterized subclass
    assertThrows(RuntimeException.class, () -> new Reified());
  }

  @Test
  public void staticGetters() {
    // Test Class-based factory
    Reified<String> str = Reified.get(String.class);
    assertEquals(String.class, str.getRawType());
    assertEquals(String.class, str.getType());

    // Test Type-based factory
    Type type = new Reified<Map<String, Integer>>() {}.getType();
    Reified<?> map = Reified.get(type);
    assertEquals(Map.class, map.getRawType());
  }

  @Test
  public void rawTypeHelper() {
    assertEquals(String.class, Reified.rawType(String.class));

    Type listType = new Reified<List<Integer>>() {}.getType();
    assertEquals(List.class, Reified.rawType(listType));
  }

  @Test
  public void collectionFactories() {
    // List
    assertEquals("java.util.List<java.lang.String>", Reified.list(String.class).toString());
    assertEquals(
        "java.util.List<java.lang.Integer>", Reified.list(Reified.get(Integer.class)).toString());

    // Set
    assertEquals("java.util.Set<java.lang.String>", Reified.set(String.class).toString());
    assertEquals(
        "java.util.Set<java.lang.Integer>", Reified.set(Reified.get(Integer.class)).toString());

    // Optional
    assertEquals("java.util.Optional<java.lang.String>", Reified.optional(String.class).toString());
    assertEquals(
        "java.util.Optional<java.lang.Integer>",
        Reified.optional(Reified.get(Integer.class)).toString());

    // Map
    assertEquals(
        "java.util.Map<java.lang.String, java.lang.Integer>",
        Reified.map(String.class, Integer.class).toString());
    assertEquals(
        "java.util.Map<java.lang.Double, java.lang.Boolean>",
        Reified.map(Reified.get(Double.class), Reified.get(Boolean.class)).toString());

    // CompletableFuture
    assertEquals(
        "java.util.concurrent.CompletableFuture<java.lang.String>",
        Reified.completableFuture(String.class).toString());
  }

  @Test
  public void parameterizedTypeHelpers() {
    Reified<List<String>> list = Reified.getParameterized(List.class, String.class);
    assertEquals(List.class, list.getRawType());

    Reified<Set<String>> set = Reified.getParameterized(Set.class, Reified.get(String.class));
    assertEquals(Set.class, set.getRawType());
  }

  @Test
  public void equalsAndHashCode() {
    Reified<List<String>> list1 = new Reified<List<String>>() {};
    Reified<List<String>> list2 = Reified.list(String.class);
    Reified<List<Integer>> list3 = Reified.list(Integer.class);

    assertEquals(list1, list2);
    assertEquals(list1.hashCode(), list2.hashCode());

    assertNotEquals(list1, list3);
    assertNotEquals(list1, "not a reified");
    assertNotEquals(list1, null);
  }

  @Test
  public void toStringTest() {
    assertEquals("java.lang.String", Reified.get(String.class).toString());
  }
}
