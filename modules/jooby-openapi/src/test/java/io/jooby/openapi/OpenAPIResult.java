/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import java.util.List;
import java.util.stream.Collectors;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenAPIResult {
  public final OpenAPIExt openAPI;
  private RuntimeException failure;

  public OpenAPIResult(OpenAPIExt openAPI) {
    this.openAPI = openAPI;
  }

  public RouteIterator iterator(boolean ignoreArgs) {
    return new RouteIterator(openAPI == null ? List.of() : openAPI.getOperations(), ignoreArgs);
  }

  public String toYaml() {
    return toYaml(true);
  }

  public String toYaml(boolean validate) {
    if (failure != null) {
      throw failure;
    }
    try {
      String yaml = Yaml.mapper().writeValueAsString(openAPI);
      if (validate) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml);
        if (result.getMessages().isEmpty()) {
          return yaml;
        }
        throw new IllegalStateException(
            "Invalid OpenAPI specification:\n\t- "
                + String.join("\n\t- ", result.getMessages()).trim()
                + "\n\n"
                + yaml);
      }
      return yaml;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public String toJson() {
    return toJson(true);
  }

  public String toJson(boolean validate) {
    if (failure != null) {
      throw failure;
    }
    try {
      String json = Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
      if (validate) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json);
        if (result.getMessages().isEmpty()) {
          return json;
        }
        throw new IllegalStateException(
            "Invalid OpenAPI specification:\n\t- "
                + result.getMessages().stream().collect(Collectors.joining("\n\t- ")).trim()
                + "\n\n"
                + json);
      }
      return json;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public static OpenAPIResult failure(RuntimeException failure) {
    var result = new OpenAPIResult(null);
    result.failure = failure;
    return result;
  }
}
