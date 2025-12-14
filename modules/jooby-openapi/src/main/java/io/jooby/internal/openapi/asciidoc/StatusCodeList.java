/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.openapi.asciidoc.display.MapToAsciiDoc;

@JsonIncludeProperties({"codes"})
public record StatusCodeList(List<Map<String, Object>> codes)
    implements Iterable<Map<String, Object>>, ToAsciiDoc {
  @NonNull @Override
  public String toString() {
    return codes.toString();
  }

  @NonNull @Override
  public Iterator<Map<String, Object>> iterator() {
    return codes.iterator();
  }

  @Override
  public String list(Map<String, Object> options) {
    var sb = new StringBuilder();
    codes.forEach(
        (row) ->
            sb.append("* `+")
                .append(row.get("code"))
                .append("+`: ")
                .append(row.get("reason"))
                .append('\n'));
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  @Override
  public String table(Map<String, Object> options) {
    return new MapToAsciiDoc(codes).table(options);
  }
}
