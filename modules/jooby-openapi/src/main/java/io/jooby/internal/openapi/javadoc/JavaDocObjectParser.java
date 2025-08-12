/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A utility class to parse a list of strings into a nested map structure. It supports multiple data
 * rows, dot-separated path expressions, repeated paths, and values explicitly wrapped in square
 * brackets '[]'. The structure can be augmented by subsequent rows.
 */
public class JavaDocObjectParser {

  /**
   * A parser for a non-standard JSON-like syntax where string keys and values are not enclosed in
   * quotation marks.
   *
   * <p>This parser supports: - Key-value pairs (e.g., key:value) - Nested objects (text wrapped
   * with {}) - Arrays (text wrapped with []) - All other non-object, non-array values are parsed as
   * Strings.
   *
   * <p>Note: This is a simplified parser. For standard JSON.
   */
  public static class UnquotedJsonParser {

    private String text;
    private int index = 0;
    private static final Pattern WS = Pattern.compile("\\s");

    /**
     * Parses the given unquoted JSON-like string into a Map.
     *
     * @param input The string to parse.
     * @return A Map representing the parsed structure.
     * @throws IllegalArgumentException if the input format is invalid.
     */
    public Object parse(String input) {
      if (input == null || input.trim().isEmpty()) {
        throw new IllegalArgumentException("Input string cannot be null or empty.");
      }
      this.text = input.trim();
      this.index = 0;
      if (text.startsWith("{") && text.endsWith("}")) {
        return parseObject();
      } else if (text.startsWith("[") && text.endsWith("]")) {
        return parseArray();
      }
      // generate a literal only for value without spaces
      return WS.matcher(input).find() ? input : parseLiteral();
    }

    /**
     * Parses an object structure, starting from the current index. An object is expected to be
     * enclosed in '{' and '}'.
     */
    private Map<String, Object> parseObject() {
      Map<String, Object> map = new LinkedHashMap<>();

      expectChar('{');
      skipWhitespace();

      while (peek() != '}') {
        String key = parseKey();
        skipWhitespace();
        expectChar(':');
        skipWhitespace();
        Object value = parseValue();
        map.put(key, value);
        skipWhitespace();

        if (peek() == ',') {
          consumeChar();
          skipWhitespace();
        } else if (peek() != '}') {
          throw new IllegalArgumentException("Expected ',' or '}' after value at index " + index);
        }
      }

      expectChar('}');
      return map;
    }

    /**
     * Parses an array structure, starting from the current index. An array is expected to be
     * enclosed in '[' and ']'.
     */
    private List<Object> parseArray() {
      List<Object> list = new ArrayList<>();

      expectChar('[');
      skipWhitespace();

      while (peek() != ']') {
        list.add(parseValue());
        skipWhitespace();

        if (peek() == ',') {
          consumeChar();
          skipWhitespace();
        } else if (peek() != ']') {
          throw new IllegalArgumentException(
              "Expected ',' or ']' after value in array at index " + index);
        }
      }

      expectChar(']');
      return list;
    }

    /**
     * Parses a key from the input string. A key is a sequence of characters up to the colon ':'.
     */
    private String parseKey() {
      int start = index;
      while (index < text.length() && text.charAt(index) != ':') {
        index++;
      }
      if (index == start) {
        throw new IllegalArgumentException("Found empty key at index " + start);
      }
      return text.substring(start, index).trim();
    }

    /**
     * Determines the type of the value at the current index and parses it. It can be an object, an
     * array, or a string literal.
     */
    private Object parseValue() {
      skipWhitespace();
      char currentChar = peek();

      if (currentChar == '{') {
        return parseObject();
      } else if (currentChar == '[') {
        return parseArray();
      } else {
        return parseLiteral();
      }
    }

    /**
     * Parses a literal value as a string. The literal ends at the next comma ',', closing brace
     * '}', or closing bracket ']'.
     */
    private Object parseLiteral() {
      int start = index;
      while (index < text.length()
          && text.charAt(index) != ','
          && text.charAt(index) != '}'
          && text.charAt(index) != ']') {
        index++;
      }
      var literal = text.substring(start, index).trim();
      try {
        return Long.parseLong(literal);
      } catch (NumberFormatException ignored) {
        try {
          return Double.parseDouble(literal);
        } catch (NumberFormatException ignored2) {
          return "true".equals(literal)
              ? Boolean.TRUE
              : "false".equals(literal) ? Boolean.FALSE : literal;
        }
      }
    }

    // --- Utility Methods ---

    private char peek() {
      if (index >= text.length()) {
        throw new IllegalArgumentException("Unexpected end of input.");
      }
      return text.charAt(index);
    }

    private void consumeChar() {
      index++;
    }

    private void expectChar(char expected) {
      skipWhitespace();
      if (peek() != expected) {
        throw new IllegalArgumentException(
            "Expected '" + expected + "' but found '" + peek() + "' at index " + index);
      }
      consumeChar();
    }

    private void skipWhitespace() {
      while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
        index++;
      }
    }
  }

  /**
   * Parses a list containing one or more data rows into a list of maps. - A new row is started each
   * time the first key of the input list is encountered. - Subsequent rows can add new keys
   * (augment the structure) but cannot change the data type of an existing path (e.g., from an
   * object to a string). - Repeated paths within a single row will generate a list of values. - A
   * value wrapped in '[]' (e.g., "[value]") will be parsed as a list with a single element.
   *
   * @param inputList The list of strings to parse. Must not be null.
   * @return A List of Maps, where each map represents a data row.
   * @throws IllegalArgumentException if a row tries to change an existing data structure.
   */
  public static List<Map<String, Object>> parse(List<String> inputList) {
    try {
      if (inputList.isEmpty()) {
        return List.of();
      }

      // --- Step 1: Split the flat input list into a list of rows ---
      List<List<String>> allRowsPairs = new ArrayList<>();
      String rowDelimiterKey = inputList.get(0);
      List<String> currentRowPairs = new ArrayList<>();

      for (int i = 0; i < inputList.size(); i += 2) {
        String key = inputList.get(i);
        if (i + 1 >= inputList.size()) {
          break; // Ignore last key if it has no value
        }
        String value = inputList.get(i + 1);

        if (key.equals(rowDelimiterKey) && !currentRowPairs.isEmpty()) {
          allRowsPairs.add(new ArrayList<>(currentRowPairs));
          currentRowPairs.clear();
        }
        currentRowPairs.add(key);
        currentRowPairs.add(value);
      }
      if (!currentRowPairs.isEmpty()) {
        allRowsPairs.add(new ArrayList<>(currentRowPairs));
      }

      // --- Step 2: Process each row independently and add its map to the result list ---
      List<Map<String, Object>> resultList = new ArrayList<>();
      for (List<String> rowPairs : allRowsPairs) {
        resultList.add(buildMapFromPairs(rowPairs));
      }

      return resultList;
    } catch (Exception ignored) {
      // just returns empty, we won't fail the entire process.
      return List.of();
    }
  }

  /**
   * Builds a single nested map from a list of key-value pairs representing one row. This helper
   * contains the core parsing logic for a single data row.
   *
   * @param pairs A list of strings for a single data row.
   * @return A single, potentially nested, map.
   */
  private static Map<String, Object> buildMapFromPairs(List<String> pairs) {
    Map<String, Object> resultMap = new LinkedHashMap<>();

    // Step 1: Group all values by their full path.
    Map<String, List<String>> pathValues = new LinkedHashMap<>();
    for (int i = 0; i < pairs.size(); i += 2) {
      pathValues.computeIfAbsent(pairs.get(i), k -> new ArrayList<>()).add(pairs.get(i + 1));
    }

    // Step 2: Group paths by their first segment (e.g., "parents" from "parents.name").
    Map<String, List<String>> groupedByFirstSegment = new LinkedHashMap<>();
    for (String path : pathValues.keySet()) {
      String firstSegment = path.split("\\.")[0];
      groupedByFirstSegment.computeIfAbsent(firstSegment, k -> new ArrayList<>()).add(path);
    }

    // Step 3: Build the map by processing each group.
    for (Map.Entry<String, List<String>> entry : groupedByFirstSegment.entrySet()) {
      String groupKey = entry.getKey();
      List<String> pathsInGroup = entry.getValue();

      boolean isList = pathsInGroup.stream().anyMatch(p -> pathValues.get(p).size() > 1);
      boolean isSimpleList = pathsInGroup.size() == 1 && pathsInGroup.get(0).equals(groupKey);

      if (isList && isSimpleList) {
        // Case 1: A list of simple values (e.g., "tags": ["a", "b"]).
        List<String> values = pathValues.get(groupKey);
        resultMap.put(groupKey, values);
      } else if (isList) {
        // Case 2: A list of nested objects (e.g., "parents": [{...}, {...}]).
        int rowCount = pathValues.get(pathsInGroup.get(0)).size();
        List<Map<String, Object>> listOfMaps = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
          listOfMaps.add(new LinkedHashMap<>());
        }

        for (String path : pathsInGroup) {
          List<String> values = pathValues.get(path);
          if (values.size() != rowCount) {
            throw new IllegalArgumentException(
                "Mismatched value count for properties of '" + groupKey + "'.");
          }
          String innerPath = path.substring(groupKey.length() + 1);
          for (int i = 0; i < rowCount; i++) {
            setValue(listOfMaps.get(i), innerPath, values.get(i));
          }
        }
        resultMap.put(groupKey, listOfMaps);
      } else {
        // Case 3: Simple key-value pairs, possibly nested (e.g., "address.country").
        for (String path : pathsInGroup) {
          String value = pathValues.get(path).get(0);
          setValue(resultMap, path, parseJson(value));
        }
      }
    }
    return resultMap;
  }

  public static Object parseJson(String value) {
    try {
      return new UnquotedJsonParser().parse(value);
    } catch (Exception ignored) {
      // just returns the input value, we won't fail the entire process
      return value;
    }
  }

  /**
   * A helper method to place a value into a nested map structure based on a dot-separated path. It
   * creates nested maps as needed and throws an exception on path conflicts.
   *
   * @param map The root map to modify.
   * @param path The dot-separated path (e.g., "address.country").
   * @param value The value to set at the specified path.
   */
  @SuppressWarnings("unchecked")
  private static void setValue(Map<String, Object> map, String path, Object value) {
    String[] parts = path.split("\\.");
    Map<String, Object> currentMap = map;

    for (int i = 0; i < parts.length - 1; i++) {
      String part = parts[i];
      Object node = currentMap.computeIfAbsent(part, k -> new LinkedHashMap<String, Object>());

      if (!(node instanceof Map)) {
        throw new IllegalArgumentException(
            "Path conflict: '" + part + "' contains a value and cannot be treated as a map.");
      }
      currentMap = (Map<String, Object>) node;
    }

    String finalKey = parts[parts.length - 1];
    if (!currentMap.containsKey(finalKey)) {
      // Don't override
      currentMap.put(finalKey, value);
    }
  }
}
