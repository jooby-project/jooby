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

public class TinyYamlDocParser {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parse(List<String> properties) {
    // The root of our final tree structure.
    var root = new LinkedHashMap<String, Object>();

    for (int i = 0; i < properties.size(); i += 2) {
      var keyPath = properties.get(i);
      var value = properties.get(i + 1);
      var keys = keyPath.split("\\.");

      Map<String, Object> currentNode = root;
      for (int j = 0; j < keys.length - 1; j++) {
        String key = keys[j];
        Object nextNode =
            currentNode.computeIfAbsent(key, k -> new LinkedHashMap<String, Object>());
        currentNode = (Map<String, Object>) nextNode;
      }
      var finalKey = keys[keys.length - 1];
      @SuppressWarnings("unchecked")
      List<String> values =
          (List<String>) currentNode.computeIfAbsent(finalKey, k -> new ArrayList<String>());
      values.add(value);
    }
    var result = restructureNode(root);
    if (result instanceof Map) {
      return (Map<String, Object>) result;
    }
    throw new IllegalArgumentException("Unable to parse: " + properties);
  }

  /**
   * Recursively traverses the tree and restructures nodes where appropriate. If a map contains only
   * list-of-string values of the same size, it "zips" them into a list of maps (objects).
   *
   * @param node The current node (Map or List) to process.
   * @return The restructured node.
   */
  @SuppressWarnings("unchecked")
  private static Object restructureNode(Object node) {
    if (!(node instanceof Map)) {
      // This is a leaf (already a List<String>), so return it as is.
      return node;
    }

    Map<String, Object> map = (Map<String, Object>) node;
    Map<String, Object> restructuredMap = new LinkedHashMap<>();

    // First, recursively restructure all children of the current map.
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      var value = restructureNode(entry.getValue());
      var propertyKey = entry.getKey();
      restructuredMap.put(propertyKey, value);
    }

    // Now, check if the current node itself should be restructured.
    if (restructuredMap.isEmpty()) {
      return restructuredMap;
    }

    // Check if all values in the map are lists of strings.
    boolean canBeZipped = true;
    int listSize = -1;

    for (var value : restructuredMap.values()) {
      if (!(value instanceof List)
          || ((List<?>) value).isEmpty()
          || !(((List<?>) value).getFirst() instanceof String)) {
        canBeZipped = false;
        break;
      }
      List<String> list = (List<String>) value;
      if (listSize == -1) {
        listSize = list.size();
      } else if (listSize != list.size()) {
        // If lists have different sizes, they can't be zipped together.
        canBeZipped = false;
        break;
      }
    }

    // If the conditions are met, perform the "zip" operation.
    if (canBeZipped) {
      List<Map<String, String>> listOfObjects = new ArrayList<>();
      for (int i = 0; i < listSize; i++) {
        Map<String, String> objectMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : restructuredMap.entrySet()) {
          objectMap.put(nameNoDash(entry.getKey()), ((List<String>) entry.getValue()).get(i));
        }
        listOfObjects.add(objectMap);
      }
      if (listOfObjects.size() == 1
          && restructuredMap.keySet().stream().noneMatch(TinyYamlDocParser::startsWithDash)) {
        return listOfObjects.getFirst();
      }
      return listOfObjects;
    }
    return restructuredMap;
  }

  private static boolean startsWithDash(String name) {
    return name.charAt(0) == '-';
  }

  private static String nameNoDash(String name) {
    return startsWithDash(name) ? name.substring(1) : name;
  }
}
