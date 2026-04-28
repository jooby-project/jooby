/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class MissingValueTest {

  private ValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new ValueFactory();
  }

  @Test
  @DisplayName("Test basic properties and toString")
  void testBasicProperties() {
    MissingValue missing = new MissingValue(factory, "field");

    assertEquals("field", missing.name());
    assertEquals(0, missing.size());
    assertEquals("<missing>", missing.toString());
  }

  @Test
  @DisplayName("Test get(String) branches and get(int)")
  void testGetters() {
    MissingValue missing = new MissingValue(factory, "user");

    // Branch 1: get(String) where the requested name EXACTLY matches the current node's name
    Value sameName = missing.get("user");
    assertSame(missing, sameName); // Should return 'this'

    // Branch 2: get(String) where the requested name differs (builds a dot-notation path)
    Value differentName = missing.get("email");
    assertTrue(differentName instanceof MissingValue);
    assertEquals("user.email", differentName.name());

    // Test get(int) (builds a bracket-notation path)
    Value indexValue = missing.get(5);
    assertTrue(indexValue instanceof MissingValue);
    assertEquals("user[5]", indexValue.name());

    // Test getOrDefault
    Value defaultVal = missing.getOrDefault("age", "18");
    assertFalse(defaultVal.isMissing());
    assertEquals("18", defaultVal.value());
  }

  @Test
  @DisplayName("Test Exception throwing for scalar evaluation")
  void testExceptions() {
    MissingValue missing = new MissingValue(factory, "token");

    // value() should throw MissingValueException
    MissingValueException ex1 = assertThrows(MissingValueException.class, missing::value);
    assertTrue(ex1.getMessage().contains("token"));

    // to(Class) should throw MissingValueException
    MissingValueException ex2 =
        assertThrows(MissingValueException.class, () -> missing.to(String.class));
    assertTrue(ex2.getMessage().contains("token"));
  }

  @Test
  @DisplayName("Test empty collections, iterators, and nullable conversions")
  void testEmptyCollectionsAndOptionals() {
    MissingValue missing = new MissingValue(factory, "items");

    // Nullable resolution
    assertNull(missing.toNullable(String.class));

    // Iterators and Collections
    assertFalse(missing.iterator().hasNext());
    assertEquals(Collections.emptyMap(), missing.toMap());
    assertEquals(Collections.emptyMap(), missing.toMultimap());
    assertEquals(Collections.emptyList(), missing.toList());
    assertEquals(Collections.emptySet(), missing.toSet());

    // Typed Collections
    assertEquals(Collections.emptyList(), missing.toList(Integer.class));
    assertEquals(Collections.emptySet(), missing.toSet(Integer.class));

    // Optionals
    assertEquals(Optional.empty(), missing.toOptional());
  }

  @Test
  @DisplayName("Test equals and hashCode branches")
  void testEqualsAndHashCode() {
    MissingValue m1 = new MissingValue(factory, "param");
    MissingValue m2 = new MissingValue(factory, "param");
    MissingValue m3 = new MissingValue(factory, "other");

    // Branch: instanceof MissingValue == true, names match
    assertTrue(m1.equals(m2));
    assertEquals(m1.hashCode(), m2.hashCode());

    // Branch: instanceof MissingValue == true, names differ
    assertFalse(m1.equals(m3));

    // Branch: instanceof MissingValue == false (null and different class)
    assertFalse(m1.equals(null));
    assertFalse(m1.equals("param"));
  }
}
