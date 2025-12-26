/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.asciidoc.AsciiDocContext;
import io.jooby.internal.openapi.mcp.McpContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenAPIResult {
  public final OpenAPIExt openAPI;
  private final ObjectMapper json;
  private final ObjectMapper yaml;
  private RuntimeException failure;

  public OpenAPIResult(ObjectMapper json, ObjectMapper yaml, OpenAPIExt openAPI) {
    this.json = json;
    this.yaml = yaml;
    this.openAPI = openAPI;
  }

  public RouteIterator iterator(boolean ignoreArgs) {
    if (failure != null) {
      throw failure;
    }
    return new RouteIterator(openAPI == null ? List.of() : openAPI.getOperations(), ignoreArgs);
  }

  public OpenAPIExt getOpenAPI() {
    return openAPI;
  }

  public String toYaml() {
    return toYaml(true);
  }

  public String toYaml(boolean validate) {
    if (failure != null) {
      throw failure;
    }
    try {
      String yaml = this.yaml.writeValueAsString(openAPI);
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
    return json(validate);
  }

  private String json(boolean validate) {
    try {
      String json = this.json.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
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

  public String toAsciiDoc(Path index) {
    return toAsciiDoc(index, false);
  }

  public String toAsciiDoc(Path index, boolean validate) {
    if (failure != null) {
      throw failure;
    }
    try {
      json(validate);
      var asciiDoc = new AsciiDocContext(index.getParent(), this.json, this.yaml, openAPI);
      return asciiDoc.generate(index);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public String toMcp() {
    return toMcp(false);
  }

  public String toMcp(boolean validate) {
    if (failure != null) {
      throw failure;
    }
    try {
      json(validate);
      var mcp = new McpContext(openAPI);
      return mcp.generate();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public static OpenAPIResult failure(RuntimeException failure) {
    var result = new OpenAPIResult(Json.mapper(), Yaml.mapper(), null);
    result.failure = failure;
    return result;
  }
}
