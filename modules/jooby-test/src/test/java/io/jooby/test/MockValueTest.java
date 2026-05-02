/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class MockValueTest {

  // A simple implementation of the interface for testing purposes
  private static class SimpleMockValue implements MockValue {
    private final Object value;

    SimpleMockValue(@Nullable Object value) {
      this.value = value;
    }

    @Override
    public @Nullable Object value() {
      return value;
    }
  }

  @Test
  void testValueReturnsStoredObject() {
    Object expected = new Object();
    MockValue mockValue = new SimpleMockValue(expected);

    assertEquals(expected, mockValue.value());
  }

  @Test
  void testValueReturnsNullWhenStoredIsNull() {
    MockValue mockValue = new SimpleMockValue(null);

    assertNull(mockValue.value());
  }

  @Test
  void testTypedValueReturnsCastObject() {
    String expected = "test string";
    MockValue mockValue = new SimpleMockValue(expected);

    String result = mockValue.value(String.class);

    assertEquals(expected, result);
  }

  @Test
  void testTypedValueThrowsClassCastExceptionWhenNull() {
    MockValue mockValue = new SimpleMockValue(null);

    ClassCastException ex =
        assertThrows(ClassCastException.class, () -> mockValue.value(String.class));
    assertTrue(ex.getMessage().contains("Found: null"));
    assertTrue(ex.getMessage().contains("expected: class java.lang.String"));
  }

  @Test
  void testTypedValueThrowsClassCastExceptionWhenTypeMismatch() {
    Integer wrongTypeObject = 42;
    MockValue mockValue = new SimpleMockValue(wrongTypeObject);

    ClassCastException ex =
        assertThrows(ClassCastException.class, () -> mockValue.value(String.class));
    assertTrue(ex.getMessage().contains("Found: class java.lang.Integer"));
    assertTrue(ex.getMessage().contains("expected: class java.lang.String"));
  }

  @Test
  void testTypedValueHandlesSubclasses() {
    Number numberValue = 42; // Integer is a subclass of Number
    MockValue mockValue = new SimpleMockValue(numberValue);

    // Should successfully cast to Number
    Number result = mockValue.value(Number.class);
    assertEquals(42, result);

    // Should successfully cast to the specific Integer type as well
    Integer intResult = mockValue.value(Integer.class);
    assertEquals(42, intResult);
  }
}
