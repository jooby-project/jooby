/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ValueLikeTest {

  private ValueFactory factory = new ValueFactory();

  @Test
  public void firstElement() {
    var single = Value.value(factory, "name", "a");
    var array = Value.array(factory, "name", List.of("a", "b", "c"));
    var hash = Value.hash(factory, Map.of("a", List.of("1"), "b", List.of("2"), "c", List.of("3")));
    //    check(node -> {
    //      assertEquals("a", node.value());
    //      assertEquals("a", node.get(0).value());
    //      assertEquals("a", node.toList().get(0));
    //    }, single, array, hash);
  }

  private void check(SneakyThrows.Consumer<Value> consumer, Value... values) {
    Stream.of(values).forEach(consumer);
  }
}
