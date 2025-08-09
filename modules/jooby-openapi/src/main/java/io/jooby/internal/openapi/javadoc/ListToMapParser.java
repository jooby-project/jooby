/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A utility class to parse a list of strings into a nested map structure. It supports multiple data
 * rows, dot-separated path expressions, repeated paths, and values explicitly wrapped in square
 * brackets '[]'. The structure can be augmented by subsequent rows.
 */
public class ListToMapParser {

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
  }

  /**
   * Builds a single nested map from a list of key-value pairs representing one row. This helper
   * contains the core parsing logic for a single data row.
   *
   * @param pairs A list of strings for a single data row.
   * @return A single, potentially nested, map.
   */
  private static Map<String, Object> buildMapFromPairs(List<String> pairs) {
    Map<String, Object> resultMap = new HashMap<>();

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
          listOfMaps.add(new HashMap<>());
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
          if (value.startsWith("[") && value.endsWith("]")) {
            String unwrappedValue = value.substring(1, value.length() - 1);
            setValue(
                resultMap, path, Stream.of(unwrappedValue.split(",")).map(String::trim).toList());
          } else {
            setValue(resultMap, path, value);
          }
        }
      }
    }
    return resultMap;
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
      Object node = currentMap.computeIfAbsent(part, k -> new HashMap<String, Object>());

      if (!(node instanceof Map)) {
        throw new IllegalArgumentException(
            "Path conflict: '" + part + "' contains a value and cannot be treated as a map.");
      }
      currentMap = (Map<String, Object>) node;
    }

    String finalKey = parts[parts.length - 1];
    if (currentMap.containsKey(finalKey)) {
      throw new IllegalArgumentException(
          "Path conflict: Key '" + finalKey + "' would be overwritten in the same object.");
    }
    currentMap.put(finalKey, value);
  }
}
