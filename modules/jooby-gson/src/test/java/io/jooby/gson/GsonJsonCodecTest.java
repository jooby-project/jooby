/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import io.jooby.Reified;

class GsonJsonCodecTest {

  private GsonJsonCodec codec;

  @BeforeEach
  void setUp() {
    codec = new GsonJsonCodec(new Gson());
  }

  @Test
  void shouldEncodeMapToJson() {
    // Using a LinkedHashMap guarantees the order of the keys in the output JSON
    Map<String, Integer> map = new java.util.LinkedHashMap<>();
    map.put("Alice", 30);
    map.put("Bob", 25);

    String json = codec.encode(map);

    assertEquals("{\"Alice\":30,\"Bob\":25}", json);
  }

  @Test
  void shouldDecodeJsonToGenericMapUsingReified() {
    String json = "{\"Alice\":30,\"Bob\":25}";

    // Using the anonymous subclass trick to capture Map<String, Integer> without type erasure
    Map<String, Integer> map = codec.decode(json, Reified.map(String.class, Integer.class));

    assertNotNull(map);
    assertEquals(2, map.size());

    assertEquals(30, map.get("Alice"));
    assertEquals(25, map.get("Bob"));
  }

  @Test
  void shouldDecodeJsonToGenericListMapUsingReified() {
    String json = "[{\"Alice\":30,\"Bob\":25}]";

    // Using the anonymous subclass trick to capture Map<String, Integer> without type erasure
    List<Map<String, Integer>> list =
        codec.decode(json, Reified.list(Reified.map(String.class, Integer.class)));

    assertNotNull(list);
    assertEquals(1, list.size());

    var map = list.getFirst();

    assertEquals(30, map.get("Alice"));
    assertEquals(25, map.get("Bob"));
  }
}
