/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.value.ConversionHint;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class SingleValueTest {

  private ValueFactory factory;
  private SingleValue singleValue;
  private final String name = "myParam";
  private final String rawValue = "123";

  @BeforeEach
  void setUp() {
    factory = mock(ValueFactory.class);
    singleValue = new SingleValue(factory, name, rawValue);
  }

  @Test
  @DisplayName("Verify basic identity and size properties")
  void testIdentity() {
    assertEquals(name, singleValue.name());
    assertEquals(rawValue, singleValue.value());
    assertEquals(rawValue, singleValue.toString());
    assertEquals(1, singleValue.size());
  }

  @Test
  @DisplayName("Verify access to nested or indexed values returns MissingValue")
  void testNestedAccess() {
    // Access by string name
    Value nested = singleValue.get("child");
    assertTrue(nested instanceof MissingValue);
    assertEquals(name + ".child", nested.name());

    // Access by index (internally converts index to string)
    Value indexed = singleValue.get(0);
    assertTrue(indexed instanceof MissingValue);
    assertEquals(name + ".0", indexed.name());

    // getOrDefault
    Value def = singleValue.getOrDefault("other", "fallback");
    assertEquals("fallback", def.value());
  }

  @Test
  @DisplayName("Verify iteration over a single element")
  void testIterator() {
    Iterator<Value> it = singleValue.iterator();
    assertTrue(it.hasNext());
    assertSame(singleValue, it.next());
    assertFalse(it.hasNext());
  }

  @Test
  @DisplayName("Verify list, set, and multimap collection conversions")
  void testCollections() {
    // toMultimap
    Map<String, List<String>> multimap = singleValue.toMultimap();
    assertEquals(List.of(rawValue), multimap.get(name));

    // toList / toSet (String versions)
    assertEquals(List.of(rawValue), singleValue.toList());
    assertEquals(Set.of(rawValue), singleValue.toSet());
  }

  @Test
  @DisplayName("Verify factory conversion delegation for complex types")
  void testTypeConversions() {
    when(factory.convert(eq(Integer.class), eq(singleValue))).thenReturn(123);
    when(factory.convert(eq(Integer.class), eq(singleValue), eq(ConversionHint.Nullable)))
        .thenReturn(123);

    // to(Class)
    assertEquals(123, singleValue.to(Integer.class));

    // toNullable(Class)
    assertEquals(123, singleValue.toNullable(Integer.class));

    // toOptional(Class)
    Optional<Integer> opt = singleValue.toOptional(Integer.class);
    assertTrue(opt.isPresent());
    assertEquals(123, opt.get());

    // toList(Class) / toSet(Class)
    assertEquals(List.of(123), singleValue.toList(Integer.class));
    assertEquals(Set.of(123), singleValue.toSet(Integer.class));

    verify(factory, times(3)).convert(Integer.class, singleValue);
  }
}
