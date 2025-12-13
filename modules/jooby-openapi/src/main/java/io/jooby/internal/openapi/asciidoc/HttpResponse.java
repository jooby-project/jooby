/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jooby.StatusCode;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.ResponseExt;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

public record HttpResponse(
    AsciiDocContext context,
    OperationExt operation,
    Integer statusCode,
    Map<String, Object> options)
    implements HttpMessage {
  @Override
  public HttpParamList getHeaders() {
    return new HttpParamList(
        operation.getProduces().stream()
            .map(value -> new HttpParam("Content-Type", new StringSchema(), value, "header", null))
            .toList(),
        HttpParamList.NAME_DESC);
  }

  @Override
  public HttpParamList getCookies() {
    return new HttpParamList(List.of(), HttpParamList.NAME_DESC);
  }

  @Override
  public Schema<?> getBody() {
    return selectBody(getBody(getResponse()), options.getOrDefault("body", "full").toString());
  }

  private ResponseExt getResponse() {
    if (statusCode == null) {
      return operation.getDefaultResponse();
    } else {
      var rsp = operation.getResponses().get(Integer.toString(statusCode));
      if (rsp == null) {
        if (statusCode >= 200 && statusCode <= 299) {
          // override default response
          return operation.getDefaultResponse();
        }
      }
      return (ResponseExt) rsp;
    }
  }

  public StatusCode getStatusCode() {
    if (statusCode == null) {
      return Optional.ofNullable(getResponse())
          .map(it -> StatusCode.valueOf(Integer.parseInt(it.getCode())))
          .orElse(StatusCode.OK);
    }
    return StatusCode.valueOf(statusCode);
  }

  @SuppressWarnings("unchecked")
  private Schema<?> getBody(ResponseExt response) {
    return Optional.ofNullable(response)
        .map(it -> toSchema(it.getContent(), List.of()))
        .map(context::resolveSchema)
        .orElse(AsciiDocContext.EMPTY_SCHEMA);
  }
}
