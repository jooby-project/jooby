/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jooby.StatusCode;
import io.jooby.internal.openapi.*;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class OperationBuilder {

  private ApiResponses responses;

  private final OperationExt operation = mock(OperationExt.class);

  static {
    ModelConverters.getInstance().addConverter(new ModelConverterExt(Json.mapper()));
  }

  public static OperationBuilder operation(String method, String pattern) {
    return new OperationBuilder().method(method).pattern(pattern);
  }

  public OperationBuilder query(String... name) {
    return parameter(Map.of("query", mapOf(name)));
  }

  public OperationBuilder form(String... name) {
    return parameter(Map.of("form", mapOf(name)));
  }

  private static Map<String, String> mapOf(String... values) {
    Map<String, String> map = new LinkedHashMap<>();
    for (var value : values) {
      map.put(value, "string");
    }
    return map;
  }

  public OperationBuilder parameter(Map<String, Map<String, String>> parameterSpecs) {
    List<Parameter> parameters = new ArrayList<>();
    for (var parameterSpec : parameterSpecs.entrySet()) {
      var in = parameterSpec.getKey();
      for (var entry : parameterSpec.getValue().entrySet()) {
        var schema = mock(Schema.class);
        var type = entry.getValue();
        if (type.equals("binary")) {
          when(schema.getFormat()).thenReturn(type);
          type = "string";
        }
        when(schema.getType()).thenReturn(type);
        var parameter = mock(ParameterExt.class);
        when(parameter.getName()).thenReturn(entry.getKey());
        when(parameter.getIn()).thenReturn(in);
        when(parameter.getSchema()).thenReturn(schema);
        parameters.add(parameter);
      }
    }
    when(operation.getParameters()).thenReturn(parameters);
    return this;
  }

  public OperationBuilder produces(String... produces) {
    return produces(List.of(produces));
  }

  public OperationBuilder produces(List<String> produces) {
    when(operation.getProduces()).thenReturn(produces);
    return this;
  }

  public OperationBuilder consumes(String... consumes) {
    return consumes(List.of(consumes));
  }

  public OperationBuilder consumes(List<String> consumes) {
    when(operation.getConsumes()).thenReturn(consumes);
    return this;
  }

  public OperationBuilder method(String method) {
    when(operation.getMethod()).thenReturn(method);
    return this;
  }

  public OperationBuilder pattern(String pattern) {
    when(operation.getPattern()).thenReturn(pattern);
    return this;
  }

  public OperationBuilder response(Object body, StatusCode code, String contentType) {
    var schemas = ModelConvertersExt.getInstance().read(body.getClass());
    var mediaType = new io.swagger.v3.oas.models.media.MediaType();
    mediaType.schema(schemas.get(body.getClass().getSimpleName()));

    var content = new Content();
    content.addMediaType(contentType, mediaType);

    ResponseExt response = mock(ResponseExt.class);
    when(response.getContent()).thenReturn(content);
    when(response.getCode()).thenReturn(Integer.toString(code.value()));

    if (responses == null) {
      responses = mock(ApiResponses.class);
      when(operation.getResponses()).thenReturn(responses);
      when(operation.getDefaultResponse()).thenReturn(response);
    }
    when(responses.get(Integer.toString(code.value()))).thenReturn(response);
    return this;
  }

  public OperationBuilder defaultResponse() {
    return response(Map.of(), StatusCode.OK, "application/json");
  }

  public OperationBuilder body(Object body, String contentType) {
    consumes(contentType);
    var schemas = ModelConverters.getInstance().read(body.getClass());
    var mediaType = new io.swagger.v3.oas.models.media.MediaType();
    mediaType.schema(schemas.get(body.getClass().getSimpleName()));

    var content = new Content();
    content.addMediaType(contentType, mediaType);

    var requestBodyExt = mock(RequestBodyExt.class);
    when(requestBodyExt.getContent()).thenReturn(content);
    when(requestBodyExt.getJavaType()).thenReturn(body.getClass().getName());
    when(operation.getRequestBody()).thenReturn(requestBodyExt);
    return this;
  }

  public OperationExt build() {
    return operation;
  }
}
