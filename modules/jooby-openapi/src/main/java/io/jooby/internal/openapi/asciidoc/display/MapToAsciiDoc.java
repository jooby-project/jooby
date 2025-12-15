/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc.display;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jooby.internal.openapi.asciidoc.ToAsciiDoc;

public record MapToAsciiDoc(List<Map<String, Object>> rows) implements ToAsciiDoc {

  public String list(Map<String, Object> options) {
    var sb = new StringBuilder();
    rows.forEach(
        (row) -> {
          row.forEach(
              (name, value) -> {
                sb.append("* ").append(name).append(": ").append(value).append('\n');
              });
        });
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  public String table(Map<String, Object> options) {
    var sb = new StringBuilder();
    if (!options.isEmpty()) {
      sb.append(
              options.entrySet().stream()
                  .map(it -> it.getKey() + "=\"" + it.getValue() + "\"")
                  .collect(Collectors.joining(", ", "[", "]")))
          .append('\n');
    }
    sb.append("|===").append('\n');
    if (!rows.isEmpty()) {
      sb.append(rows.getFirst().keySet().stream().collect(Collectors.joining("|", "|", "")))
          .append("\n\n");
      rows.forEach(
          row -> {
            row.values().forEach(value -> sb.append("|").append(value).append("\n"));
            sb.append("\n");
          });
      sb.append("\n");
    }
    sb.setLength(sb.length() - 1);
    sb.append("|===");
    return sb.toString();
  }
}
