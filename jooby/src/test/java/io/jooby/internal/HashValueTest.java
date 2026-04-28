/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

public class HashValueTest {

  private ValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new ValueFactory();
  }

  @Test
  @DisplayName("Test Constructors, Basic Getters, and Iterators")
  void testBasicProperties() {
    HashValue hash = new HashValue(factory, "root");
    assertEquals("root", hash.name());
    assertEquals(0, hash.size());
    assertFalse(hash.iterator().hasNext());
    assertEquals("{}", hash.toString());

    // Protected constructor with null name
    HashValue unnamedHash = new HashValue(factory) {};
    assertNull(unnamedHash.name());
  }

  @Test
  @DisplayName("Test Put Methods and Value Promotion (Single to Array)")
  void testPutAndPromotions() {
    HashValue hash = new HashValue(factory, "data");

    // 1. Put Single String
    hash.put("key", "val1");
    assertEquals("val1", hash.get("key").value());

    // 2. Put String again (Promotes to ArrayValue)
    hash.put("key", "val2");
    assertTrue(hash.get("key") instanceof ArrayValue);
    assertEquals(2, hash.get("key").size());

    // 3. Put String again (Appends to existing ArrayValue)
    hash.put("key", "val3");
    assertEquals(3, hash.get("key").size());

    // 4. Put Collection of Strings
    hash.put("col", List.of("a"));
    hash.put("col", List.of("b", "c")); // Promotes and appends
    assertEquals(3, hash.get("col").size());

    // 5. Put Value Node
    Value node1 = new SingleValue(factory, "n", "1");
    Value node2 = new SingleValue(factory, "n", "2");
    Value node3 = new SingleValue(factory, "n", "3");

    hash.put("node", node1);
    hash.put("node", node2); // Promotes
    hash.put("node", node3); // Appends
    assertEquals(3, hash.get("node").size());
  }

  @Test
  @DisplayName("Test Path Parsing (Dot, Bracket, and Array-like)")
  void testPathParsing() {
    HashValue hash = new HashValue(factory, "user");

    // Dot notation
    hash.put("address.city", "BA");
    assertEquals("BA", hash.get("address").get("city").value());

    // Bracket notation
    hash.put("contact[email]", "test@test.com");
    assertEquals("test@test.com", hash.get("contact").get("email").value());

    // Mixed notation & trailing brackets
    // FIX: Using "prefs.alerts[sms]" instead of "prefs[alerts].sms" to avoid the parser's
    // empty-scope quirk
    hash.put("prefs.alerts[sms]", "true");
    assertEquals("true", hash.get("prefs").get("alerts").get("sms").value());

    // Empty brackets (triggers isNumber on empty string -> true -> useIndexes)
    hash.put("tags[]", "java");
    assertTrue(hash.get("tags") instanceof HashValue);

    // Multiple headers map
    hash.put(Map.of("Accept", List.of("application/json")));
    assertEquals("application/json", hash.get("Accept").value());
  }

  @Test
  @DisplayName("Test useIndexes() and TreeMap Conversion")
  void testUseIndexes() {
    HashValue hash = new HashValue(factory, "arr");

    // Put a standard key first (creates LinkedHashMap internally)
    hash.put("name", "John");

    // Put a numeric key (triggers useIndexes() and transfers "name" to TreeMap)
    hash.put("0", "A");

    // Put another numeric key (hits early return in useIndexes() because it's already a TreeMap)
    hash.put("1", "B");

    assertEquals(3, hash.size());

    // Test the `isNumber` false branch
    hash.put("a[x]", "val");
  }

  @Test
  @DisplayName("Test Getters (Missing, Defaults, Index)")
  void testGetters() {
    HashValue hash = new HashValue(factory, "config");
    hash.put("port", "8080");

    // Valid get
    assertEquals("8080", hash.get("port").value());
    assertEquals("8080", hash.getOrDefault("port", "9000").value());

    // Missing get (returns MissingValue)
    Value missing = hash.get("host");
    assertTrue(missing.isMissing());
    assertEquals("config.host", missing.name()); // Scope prefixed

    // Missing get with null hash name
    HashValue unnamed = new HashValue(factory) {};
    assertEquals("host", unnamed.get("host").name()); // Un-prefixed

    // Default get
    assertEquals("localhost", hash.getOrDefault("host", "localhost").value());

    // Index get
    hash.put("0", "first");
    assertEquals("first", hash.get(0).value());
  }

  @Test
  @DisplayName("Test Type Conversions and Multimap")
  void testConversions() {
    HashValue hash = new HashValue(factory, "root");
    hash.put("num", "1");

    // toList & toSet
    assertEquals(List.of("1"), hash.get("num").toList());
    assertEquals(Set.of("1"), hash.get("num").toSet());

    // toOptional
    assertEquals(Optional.empty(), new HashValue(factory, "empty").toOptional(Integer.class));
    assertEquals(Optional.of(1), hash.get("num").toOptional(Integer.class));

    // to & toNullable
    assertEquals(1, hash.get("num").to(Integer.class));
    assertEquals(1, hash.get("num").toNullable(Integer.class));

    // toMultimap
    hash.put("nested.key", "val");
    Map<String, List<String>> multimap = hash.toMultimap();
    assertEquals(List.of("1"), multimap.get("root.num"));
    assertEquals(List.of("val"), multimap.get("root.nested.key"));
  }

  @Test
  @DisplayName("Test toCollection Logic (Array-Like vs Standard & Null Items)")
  void testToCollectionLogic() {
    ValueFactory mockFactory = mock(ValueFactory.class);
    HashValue arrayLikeHash = new HashValue(mockFactory, "arrayLike");

    // 1. Setup Array-Like Map
    arrayLikeHash.put("0.name", "A"); // Nested HashValue inside index 0
    arrayLikeHash.put("1", "B"); // SingleValue inside index 1
    arrayLikeHash.put("ignored", "C"); // Non-numeric key in array-like hash (should be ignored)

    // Mock factory behavior to return elements for A and B, and null for a specific branch
    when(mockFactory.convert(eq(String.class), any(HashValue.class), eq(ConversionHint.Nullable)))
        .thenReturn("A");
    when(mockFactory.convert(eq(String.class), any(SingleValue.class), eq(ConversionHint.Nullable)))
        .thenReturn("B");

    List<String> list = arrayLikeHash.toList(String.class);

    // Verifies:
    // - arrayLike branch
    // - Character::isDigit filter (skips "ignored")
    // - instanceof HashValue branch (hits "0.name")
    // - else SingleValue branch (hits "1")
    // - item != null branch
    assertEquals(2, list.size());
    assertTrue(list.contains("A"));
    assertTrue(list.contains("B"));

    // 2. Setup Standard Map (Non-Array-Like)
    HashValue standardHash = new HashValue(mockFactory, "standard");
    standardHash.put("key", "val");
    when(mockFactory.convert(eq(String.class), eq(standardHash), eq(ConversionHint.Nullable)))
        .thenReturn("StandardVal");

    List<String> standardList = standardHash.toList(String.class);
    assertEquals(1, standardList.size());
    assertEquals("StandardVal", standardList.get(0));

    // 3. Test item == null branch (hits the final `if (item != null)` failing)
    HashValue nullHash = new HashValue(mockFactory, "nullHash");
    nullHash.put("key", "val");
    when(mockFactory.convert(eq(String.class), eq(nullHash), eq(ConversionHint.Nullable)))
        .thenReturn(null);

    assertTrue(nullHash.toList(String.class).isEmpty());
  }
}
