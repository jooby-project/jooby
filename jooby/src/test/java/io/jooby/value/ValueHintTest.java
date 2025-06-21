/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import static io.jooby.internal.UrlParser.queryString;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.exception.ProvisioningException;
import io.jooby.exception.TypeMismatchException;

public class ValueHintTest {

  public record Search(String q, String fq, Integer start, Integer end) {}

  public record SearchPrimitive(String q, String fq, int start, int end) {}

  @Test
  public void queryHint() {
    var factory = new ValueFactory();
    var node = queryString(factory, "");
    var throwable = assertThrows(TypeMismatchException.class, () -> node.to(Search.class));
    assertEquals(
        "Cannot convert value: 'null', to: 'io.jooby.value.ValueHintTest$Search'",
        throwable.getMessage());
    // Nullable
    assertNull(node.toNullable(Search.class));
    // Empty
    var search = node.toEmpty(Search.class);
    // There is no match still query produces an empty instance
    assertNotNull(search);
    assertNull(search.q());
    assertNull(search.fq());
    assertNull(search.start);
    assertNull(search.end);
    // Strict
    assertThrows(TypeMismatchException.class, () -> factory.convert(Search.class, node));
    // Nullable
    assertNull(factory.convert(Search.class, node, ConversionHint.Nullable));
    // Default instance
    assertNotNull(factory.convert(Search.class, node, ConversionHint.Empty));
  }

  @Test
  public void queryWithNonNullProperties() {
    var factory = new ValueFactory();
    var node = queryString(factory, "");
    var throwable = assertThrows(ProvisioningException.class, () -> node.to(SearchPrimitive.class));
    assertEquals(
        "Unable to provision parameter: 'start: int', require by: constructor"
            + " io.jooby.value.ValueHintTest.SearchPrimitive(java.lang.String, java.lang.String,"
            + " int, int)",
        throwable.getMessage());
  }
}
