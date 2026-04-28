/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ArrayValueTest {

  private ValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new ValueFactory();
  }

  @Test
  @DisplayName("Test basic accessors, add variants, and iterator")
  void basicOperations() {
    ArrayValue array = new ArrayValue(factory, "tags");

    // Add variants
    array.add("java"); // String
    array.add(List.of("kotlin", "scala")); // List<String>
    array.add(Value.value(factory, "tags", "groovy")); // Value

    assertEquals("tags", array.name());
    assertEquals(4, array.size());
    assertTrue(array.toString().contains("java"));
    assertTrue(array.iterator().hasNext());
  }

  @Test
  @DisplayName("Test get(int), get(String), and getOrDefault branches")
  void getOperations() {
    ArrayValue array = new ArrayValue(factory, "tags").add(List.of("a", "b"));

    // Valid index
    assertEquals("a", array.get(0).value());

    // Invalid index (catches IndexOutOfBoundsException -> returns MissingValue)
    Value missingIndex = array.get(5);
    assertTrue(missingIndex.isMissing());
    assertEquals("tags[5]", missingIndex.name());

    // Object lookup on an array (always returns MissingValue)
    Value missingObject = array.get("prop");
    assertTrue(missingObject.isMissing());
    assertEquals("tags.prop", missingObject.name());

    // getOrDefault
    assertEquals("defaultProp", array.getOrDefault("prop", "defaultProp").value());
  }

  @Test
  @DisplayName("Test value() evaluation and ternary branch for null name")
  void valueEvaluation() {
    // Standard name branch
    ArrayValue array = new ArrayValue(factory, "tags");
    assertThrows(TypeMismatchException.class, array::value);

    // Null name branch (triggers fallback to getClass().getSimpleName())
    ArrayValue unnamedArray = new ArrayValue(factory, null);
    assertThrows(TypeMismatchException.class, unnamedArray::value);
  }

  @Test
  @DisplayName("Test Type Conversions: to, toNullable, toOptional")
  void typeConversions() {
    ArrayValue array = new ArrayValue(factory, "nums").add(List.of("1", "2"));

    // to(Class)
    assertEquals(1, array.to(Integer.class));

    // toNullable(Class) - Empty branch vs Populated branch
    ArrayValue emptyArray = new ArrayValue(factory, "empty");
    assertNull(emptyArray.toNullable(Integer.class));
    assertEquals(1, array.toNullable(Integer.class));

    // toOptional(Class) - Happy path
    assertEquals(Optional.of(1), array.toOptional(Integer.class));

    // toOptional(Class) - Exception path (catches MissingValueException)
    ValueFactory mockFactory = mock(ValueFactory.class);
    when(mockFactory.convert(any(), any(), any())).thenThrow(new MissingValueException("mock"));
    ArrayValue exceptionArray = new ArrayValue(mockFactory, "err").add("1");

    assertEquals(Optional.empty(), exceptionArray.toOptional(Integer.class));
  }

  @Test
  @DisplayName("Test toList() switch optimizations (sizes 0, 1, 2, 3, default)")
  void toListSwitchOptimizations() {
    ArrayValue a0 = new ArrayValue(factory, "a0");
    assertEquals(List.of(), a0.toList());

    ArrayValue a1 = new ArrayValue(factory, "a1").add("1");
    assertEquals(List.of("1"), a1.toList());

    ArrayValue a2 = new ArrayValue(factory, "a2").add(List.of("1", "2"));
    assertEquals(List.of("1", "2"), a2.toList());

    ArrayValue a3 = new ArrayValue(factory, "a3").add(List.of("1", "2", "3"));
    assertEquals(List.of("1", "2", "3"), a3.toList());

    ArrayValue a4 = new ArrayValue(factory, "a4").add(List.of("1", "2", "3", "4"));
    assertEquals(List.of("1", "2", "3", "4"), a4.toList()); // Hits 'default' -> collect()
  }

  @Test
  @DisplayName("Test toSet() switch optimizations (sizes 0, 1, default)")
  void toSetSwitchOptimizations() {
    ArrayValue a0 = new ArrayValue(factory, "a0");
    assertEquals(Set.of(), a0.toSet());

    ArrayValue a1 = new ArrayValue(factory, "a1").add("1");
    assertEquals(Set.of("1"), a1.toSet());

    ArrayValue a2 = new ArrayValue(factory, "a2").add(List.of("1", "2"));
    assertEquals(Set.of("1", "2"), a2.toSet()); // Hits 'default' -> collect()
  }

  @Test
  @DisplayName("Test Typed Collections and the collect() method branches")
  void typedCollectionsAndCollect() {
    ArrayValue array = new ArrayValue(factory, "nums").add(List.of("1", "2", "1"));

    // String.class branch inside collect()
    assertEquals(List.of("1", "2", "1"), array.toList(String.class));
    assertEquals(Set.of("1", "2"), array.toSet(String.class));

    // Non-String.class branch inside collect() (triggers ValueFactory conversion)
    assertEquals(List.of(1, 2, 1), array.toList(Integer.class));
    assertEquals(Set.of(1, 2), array.toSet(Integer.class));
  }

  @Test
  @DisplayName("Test toMultimap aggregation")
  void toMultimap() {
    ArrayValue array = new ArrayValue(factory, "multi").add(List.of("a", "b"));

    Map<String, List<String>> multimap = array.toMultimap();
    assertEquals(1, multimap.size());
    assertEquals(List.of("a", "b"), multimap.get("multi"));
  }
}
