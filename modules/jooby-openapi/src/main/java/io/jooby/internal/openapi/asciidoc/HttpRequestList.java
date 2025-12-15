/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import edu.umd.cs.findbugs.annotations.NonNull;

@JsonIncludeProperties({"operations"})
public record HttpRequestList(AsciiDocContext context, List<HttpRequest> operations)
    implements Iterable<HttpRequest>, ToAsciiDoc {
  @NonNull @Override
  public Iterator<HttpRequest> iterator() {
    return operations.iterator();
  }

  @NonNull @Override
  public String toString() {
    return operations.toString();
  }

  @Override
  public String list(Map<String, Object> options) {
    var sb = new StringBuilder();
    operations.forEach(
        op ->
            sb.append("* `+")
                .append(op)
                .append("+`")
                .append(
                    Optional.ofNullable(op.getSummary()).map(summary -> ": " + summary).orElse(""))
                .append('\n'));
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  @Override
  public String table(Map<String, Object> options) {
    var sb = new StringBuilder();
    if (options.isEmpty()) {
      options.put("options", "header");
    }
    options.putIfAbsent("cols", "1,1,3a");
    sb.append(
            options.entrySet().stream()
                .map(it -> it.getKey() + "=\"" + it.getValue() + "\"")
                .collect(Collectors.joining(", ", "[", "]")))
        .append('\n');
    sb.append("|===").append('\n');
    sb.append("|").append("Method|Path|Summary").append("\n\n");
    operations.forEach(
        op ->
            sb.append("|`+")
                .append(op.getMethod())
                .append("+`\n")
                .append("|`+")
                .append(op.getPath())
                .append("+`\n")
                .append("|")
                .append(Optional.ofNullable(op.operation().getSummary()).orElse(""))
                .append("\n\n"));
    if (!sb.isEmpty()) {
      sb.append("\n");
      sb.setLength(sb.length() - 1);
    }
    sb.append("|===");
    return sb.toString();
  }
}
