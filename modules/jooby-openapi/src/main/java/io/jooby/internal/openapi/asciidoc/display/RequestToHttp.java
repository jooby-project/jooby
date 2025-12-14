/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc.display;

import java.util.Map;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.ParameterExt;
import io.jooby.internal.openapi.asciidoc.AsciiDocContext;
import io.jooby.internal.openapi.asciidoc.HttpRequest;
import io.jooby.internal.openapi.asciidoc.ToSnippet;

/**
 * [source,http,options="nowrap"] ---- ${method} ${path} HTTP/1.1 {% for h in headers -%} ${h.name}:
 * ${h.value} {% endfor -%} ${requestBody -} ----
 *
 * @param context
 * @param request
 */
public record RequestToHttp(AsciiDocContext context, HttpRequest request) implements ToSnippet {
  @Override
  public String render(Map<String, Object> options) {
    try {
      var sb = new StringBuilder();
      sb.append("[source,http,options=\"nowrap\"]").append('\n');
      sb.append("----").append('\n');
      sb.append(request.getMethod())
          .append(" ")
          .append(request.getPath())
          .append(" HTTP/1.1")
          .append('\n');
      for (var header : request.getHeaders()) {
        sb.append(header.getName())
            .append(": ")
            .append(((ParameterExt) header).getDefaultValue())
            .append('\n');
      }
      var schema = request.getBody();
      if (schema != AsciiDocContext.EMPTY_SCHEMA) {
        sb.append(context.toJson(context.schemaProperties(schema), false)).append('\n');
      }
      return sb.append("----").toString();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
