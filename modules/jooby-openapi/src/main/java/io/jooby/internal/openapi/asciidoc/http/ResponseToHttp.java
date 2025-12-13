/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc.http;

import java.util.Map;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.ParameterExt;
import io.jooby.internal.openapi.asciidoc.*;

public record ResponseToHttp(AsciiDocContext context, HttpResponse response) implements ToSnippet {
  @Override
  public String render(Map<String, Object> options) {
    try {
      var sb = new StringBuilder();
      sb.append("[source,http,options=\"nowrap\"]").append('\n');
      sb.append("----").append('\n');
      sb.append("HTTP/1.1 ")
          .append(response.getStatusCode().value())
          .append(" ")
          .append(response.getStatusCode().reason())
          .append('\n');
      for (var header : response.getHeaders()) {
        var value = ((ParameterExt) header).getDefaultValue();
        sb.append(header.getName()).append(": ").append(value).append('\n');
      }
      var schema = response.getBody();
      if (schema != AsciiDocContext.EMPTY_SCHEMA) {
        sb.append(context.getJson().writeValueAsString(context.schemaProperties(schema)))
            .append('\n');
      }
      return sb.append("----").toString();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
