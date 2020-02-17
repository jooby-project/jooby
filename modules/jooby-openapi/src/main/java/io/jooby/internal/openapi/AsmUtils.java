package io.jooby.internal.openapi;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public static Map<String, Object> arrayToMap(List values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.size(); i += 2) {
      String k = (String) values.get(i);
      Object v = values.get(i + 1);
      map.put(k, v);
    }
    return map;
  }
}
