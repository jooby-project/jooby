/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.ParameterExt;
import io.jooby.internal.openapi.ResponseExt;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.swagger.v3.oas.models.media.Schema;

@JsonIncludeProperties({"method", "path"})
public record HttpResponse(
    EvaluationContext evaluationContext,
    OperationExt operation,
    Integer statusCode,
    Map<String, Object> options)
    implements HttpMessage {
  @Override
  public ParameterList getHeaders() {
    return new ParameterList(
        operation.getProduces().stream()
            .map(value -> ParameterExt.header("Content-Type", value))
            .toList(),
        ParameterList.NAME_DESC);
  }

  @Override
  public ParameterList getCookies() {
    return new ParameterList(List.of(), ParameterList.NAME_DESC);
  }

  @Override
  public AsciiDocContext context() {
    return AsciiDocContext.from(evaluationContext);
  }

  public String getMethod() {
    return operation.getMethod();
  }

  public String getPath() {
    return operation.getPath();
  }

  @Override
  public Schema<?> getBody() {
    return selectBody(getBody(response()), options.getOrDefault("body", "full").toString());
  }

  public boolean isSuccess() {
    return statusCode != null && statusCode >= 200 && statusCode < 300;
  }

  public Object getSucessOrError() {
    var response = response();
    if (response == operation.getDefaultResponse()) {
      return getBody();
    }
    // massage error apply global error format
    var rsp = operation.getResponses().get(Integer.toString(statusCode));

    if (rsp == null) {
      // default output
      return context().error(evaluationContext, Map.of("code", statusCode));
    }
    var errorContext = new LinkedHashMap<String, Object>();
    errorContext.put("code", statusCode);
    errorContext.put("message", rsp.getDescription());
    return context().error(evaluationContext, errorContext);
  }

  private ResponseExt response() {
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
      return Optional.ofNullable(response())
          .map(it -> StatusCode.valueOf(Integer.parseInt(it.getCode())))
          .orElse(StatusCode.OK);
    }
    return StatusCode.valueOf(statusCode);
  }

  private Schema<?> getBody(ResponseExt response) {
    return Optional.ofNullable(response)
        .map(it -> toSchema(it.getContent(), List.of()))
        .map(context()::resolveSchema)
        .orElse(null);
  }

  @NonNull @Override
  public String toString() {
    return operation.getMethod() + " " + operation.getPath();
  }
}
