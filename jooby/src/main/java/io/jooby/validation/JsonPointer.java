/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transforms hibernate-validator (or avaje-validator) `propertyPath` into <a
 * href="https://www.rfc-editor.org/rfc/rfc6901.html">JSON POINTER</a> format. For example:
 *
 * <p>"person.firstName" -> "/person/firstName"
 *
 * <p>"persons[0].firstName" -> "/persons/0/firstName"
 *
 * @author kliushnichenko
 * @since 3.4.2
 */
class JsonPointer {
  private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)]");

  public static String of(String propertyPath) {
    return toJsonPointer(propertyPath);
  }

  private static String toJsonPointer(String path) {
    if (path == null || path.isEmpty()) {
      return ""; // means the whole document
    }

    List<String> parts = List.of(path.split("\\."));

    return "/" + parts.stream().map(JsonPointer::handleArrayIndex).collect(Collectors.joining("/"));
  }

  private static String handleArrayIndex(String part) {
    Matcher matcher = ARRAY_PATTERN.matcher(part);
    if (matcher.matches()) {
      return matcher.group(1) + "/" + matcher.group(2);
    }
    return part;
  }
}
