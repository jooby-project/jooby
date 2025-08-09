/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.internal.openapi.javadoc.ListToMapParser;

/**
 * JUnit 5 test class for ListToMapParser. This class provides comprehensive tests for all specified
 * use cases, including structure augmentation by subsequent rows.
 */
class ListToMapParserTest {

  @Test
  @DisplayName("Should correctly parse valid multi-row input")
  void testValidMultiRowInput() {
    List<String> input =
        List.of(
            "name",
            "Edgar",
            "address.country",
            "Argentina",
            "parents.name",
            "Ruly",
            "parents.name",
            "Rody",
            "name",
            "Pato",
            "address.country",
            "Argentina",
            "parents.name",
            "Marta",
            "parents.name",
            "Dionisio");

    List<Map<String, Object>> expected =
        List.of(
            Map.of(
                "name", "Edgar",
                "address", Map.of("country", "Argentina"),
                "parents", List.of(Map.of("name", "Ruly"), Map.of("name", "Rody"))),
            Map.of(
                "name", "Pato",
                "address", Map.of("country", "Argentina"),
                "parents", List.of(Map.of("name", "Marta"), Map.of("name", "Dionisio"))));

    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should allow a subsequent row to augment the structure with new keys")
  void testSchemaAugmentationBySubsequentRow() {
    List<String> input =
        List.of(
            "name",
            "Edgar",
            "address.country",
            "Argentina",
            "name",
            "Pato",
            "address.country",
            "Argentina",
            "status",
            "active" // "status" is a new key
            );

    List<Map<String, Object>> expected =
        List.of(
            Map.of("name", "Edgar", "address", Map.of("country", "Argentina")),
            Map.of(
                "name", "Pato",
                "address", Map.of("country", "Argentina"),
                "status", "active"));

    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should throw exception for mismatched value counts in nested lists")
  void testMismatchedNestedValueCount() {
    List<String> input =
        List.of(
            "parents.name",
            "Ruly",
            "parents.name",
            "Rody",
            "parents.age",
            "50" // Mismatch: 2 names but only 1 age
            );

    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(
        List.of(
            Map.of("parents", Map.of("name", "Ruly")),
            Map.of("parents", Map.of("name", "Rody", "age", "50"))),
        result);
  }

  @Test
  @DisplayName("Should throw exception on path conflict")
  void testPathConflict() {
    List<String> input = List.of("user", "test.user", "user.role", "admin");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              ListToMapParser.parse(input);
            });
    assertTrue(exception.getMessage().contains("contains a value and cannot be treated as a map"));
  }

  @Test
  @DisplayName("Should handle a single bracketed value as a list")
  void testBracketedValueAsList() {
    List<String> input = List.of("user", "test.user", "roles", "[guest]");
    List<Map<String, Object>> expected =
        List.of(Map.of("user", "test.user", "roles", List.of("guest")));
    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should handle a repeated simple key as a list of strings")
  void testRepeatedSimpleKey() {
    List<String> input = List.of("role", "admin", "tags", "java", "tags", "parser");
    List<Map<String, Object>> expected =
        List.of(Map.of("role", "admin", "tags", List.of("java", "parser")));
    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should return an empty list for empty input")
  void testEmptyInput() {
    List<String> input = List.of();
    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should correctly parse a single row input")
  void testSingleRowInput() {
    List<String> input = List.of("name", "Edgar", "address.country", "Argentina");
    List<Map<String, Object>> expected =
        List.of(Map.of("name", "Edgar", "address", Map.of("country", "Argentina")));
    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should ignore the last key if it has no value")
  void testInputWithOddNumberOfElements() {
    List<String> input = List.of("name", "Edgar", "status");
    List<Map<String, Object>> expected = List.of(Map.of("name", "Edgar"));
    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should correctly parse complex nested lists")
  void testComplexNestedList() {
    List<String> input =
        List.of(
            "user",
            "admin",
            "parents.name",
            "Ruly",
            "parents.age",
            "50",
            "parents.name",
            "Rody",
            "parents.age",
            "52");

    List<Map<String, Object>> expected =
        List.of(
            Map.of(
                "user",
                "admin",
                "parents",
                List.of(Map.of("name", "Ruly", "age", "50"), Map.of("name", "Rody", "age", "52"))));

    List<Map<String, Object>> result = ListToMapParser.parse(input);
    assertEquals(expected, result);
  }
}
