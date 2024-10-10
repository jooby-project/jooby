package io.jooby.validation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonPointer {
  private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)]");

  public static String of(String propertyPath) {
    return toJsonPointer(propertyPath);
  }

  private static String toJsonPointer(String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }

    List<String> parts = List.of(path.split("\\."));

    return "/" + parts.stream()
        .map(JsonPointer::handleArrayIndex)
        .collect(Collectors.joining("/"));
  }

  private static String handleArrayIndex(String part) {
    Matcher matcher = ARRAY_PATTERN.matcher(part);
    if (matcher.matches()) {
      return matcher.group(1) + "/" + matcher.group(2);
    }
    return part;
  }
}
