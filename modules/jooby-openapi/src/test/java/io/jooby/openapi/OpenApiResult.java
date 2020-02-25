package io.jooby.openapi;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.Operation;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;
import java.util.stream.Collectors;

public class OpenApiResult {
  public final OpenAPI openAPI;

  private final List<Operation> operations;

  public OpenApiResult(OpenAPI openAPI, List<Operation> operations) {
    this.openAPI = openAPI;
    this.operations = operations;
  }

  public RouteIterator iterator(boolean ignoreArgs) {
    return new RouteIterator(operations, ignoreArgs);
  }

  public String toYaml() {
    try {
      String yaml = Yaml.mapper().writeValueAsString(openAPI);
      SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml);
      if (result.getMessages().isEmpty()) {
        return yaml;
      }
      throw new IllegalStateException(
          "Invalid OpenAPI specification:\n\t- " + result.getMessages().stream()
              .collect(Collectors.joining("\n\t- ")).trim());
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
