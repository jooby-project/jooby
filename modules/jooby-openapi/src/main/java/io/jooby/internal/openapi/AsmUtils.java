package io.jooby.internal.openapi;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AsmUtils {
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
    String value = (String) annotation.get(property);
    if (value != null && value.trim().length() > 0) {
      consumer.accept(value.trim());
    }
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
