/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AsmUtils {
  public static List<AnnotationNode> findAnnotationByType(List<AnnotationNode> source,
      Class annotation) {
    return findAnnotationByType(source, Collections.singletonList(annotation.getName()));
  }

  public static List<AnnotationNode> findAnnotationByType(List<AnnotationNode> source,
      List<String> types) {
    if (source == null) {
      return Collections.emptyList();
    }
    return source.stream()
        .filter(n -> types.stream().anyMatch(t -> t.equals(Type.getType(n.desc).getClassName())))
        .collect(Collectors.toList());
  }

  public static Map<String, Object> toMap(AnnotationNode node) {
    if (node == null || node.values == null) {
      return Collections.emptyMap();
    }
    List values = node.values;
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.size(); i += 2) {
      String k = (String) values.get(i);
      Object v = values.get(i + 1);
      map.put(k, v);
    }
    return map;
  }

  public static void stringValue(Map<String, Object> annotation, String property,
      Consumer<String> consumer) {
    String value = stringValueOrNull(annotation, property);
    if (value != null) {
      consumer.accept(value.trim());
    }
  }

  public static String stringValue(Map<String, Object> annotation, String property) {
    String value = stringValueOrNull(annotation, property);
    if (value == null) {
      throw new IllegalArgumentException("Missing: " + property + " on " + annotation);
    }
    return value;
  }

  public static String stringValue(Map<String, Object> annotation, String property, String defaultValue) {
    String value = stringValueOrNull(annotation, property);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  public static String stringValueOrNull(Map<String, Object> annotation, String property) {
    String value = (String) annotation.get(property);
    if (value != null && value.trim().length() > 0) {
      return value;
    }
    return null;
  }

  public static void stringList(Map<String, Object> annotation, String property,
      Consumer<List<String>> consumer) {
    List<String> value = (List<String>) annotation.get(property);
    if (value != null && value.size() > 0) {
      consumer.accept(value);
    }
  }

  public static void annotationValue(Map<String, Object> annotation, String property,
      Consumer<Map<String, Object>> consumer) {
   annotationValue(annotation, property).ifPresent(consumer);
  }

  public static Optional<Map<String, Object>> annotationValue(Map<String, Object> annotation, String property) {
    AnnotationNode value = (AnnotationNode) annotation.get(property);
    Map<String, Object> map = toMap(value);
    if (map.size() > 0) {
      return Optional.of(map);
    }
    return Optional.empty();
  }


  public static void annotationList(Map<String, Object> annotation, String property,
      Consumer<List<Map<String, Object>>> consumer) {
    List<Map<String, Object>> values = annotationList(annotation, property);
    if (values.size() > 0) {
      consumer.accept(values);
    }
  }

  public static List<Map<String, Object>> annotationList(Map<String, Object> annotation,
      String property) {
    List<AnnotationNode> value = (List<AnnotationNode>) annotation.get(property);
    if (value == null) {
      return Collections.emptyList();
    }
    List<Map<String, Object>> values = value.stream()
        .map(AsmUtils::toMap)
        .collect(Collectors.toList());
    return values;
  }

  public static void boolValue(Map<String, Object> annotation, String property,
      Consumer<Boolean> consumer) {
    Boolean value = (Boolean) annotation.get(property);
    if (value != null) {
      consumer.accept(value);
    }
  }

  public static void intValue(Map<String, Object> annotation, String property,
      Consumer<Integer> consumer) {
    Integer value = (Integer) annotation.get(property);
    if (value != null) {
      consumer.accept(value);
    }
  }

  public static void enumValue(Map<String, Object> annotation, String property,
      Consumer<String> consumer) {
    String[] values = (String[]) annotation.get(property);
    if (values != null) {
      String value = values[1];
      consumer.accept(value);
    }
  }
}
