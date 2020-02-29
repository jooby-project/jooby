package io.jooby.openapi;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OperationExt;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;
import java.util.stream.Collectors;

public class OpenAPIResult {
  public final OpenAPIExt openAPI;

  public OpenAPIResult(OpenAPIExt openAPI) {
    this.openAPI = openAPI;
  }

  public RouteIterator iterator(boolean ignoreArgs) {
    return new RouteIterator(openAPI.getOperations(), ignoreArgs);
  }

  public String toYaml() {
    return toYaml(false);
  }

  public String toYaml(boolean validate) {
    try {
      String yaml = Yaml.mapper().writeValueAsString(openAPI);
      if (validate) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml);
        if (result.getMessages().isEmpty()) {
          return yaml;
        }
        throw new IllegalStateException(
            "Invalid OpenAPI specification:\n\t- " + result.getMessages().stream()
                .collect(Collectors.joining("\n\t- ")).trim());
      }
      return yaml;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
