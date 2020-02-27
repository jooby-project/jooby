package io.jooby.openapi;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.ClassSource;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.ParserContext;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.RouteParser;
import io.jooby.internal.openapi.TypeFactory;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OpenApiTool {

  private Set<DebugOption> debug;

  private ClassLoader classLoader;

  public OpenAPI generate(String classname) {
    ClassLoader classLoader = Optional.ofNullable(this.classLoader)
        .orElseGet(() -> getClass().getClassLoader());
    ClassSource source = new ClassSource(classLoader);

    RouteParser routes = new RouteParser();
    ParserContext ctx = new ParserContext(source, TypeFactory.fromJavaName(classname), debug);
    List<OperationExt> operations = routes.parse(ctx);

    OpenAPIExt openapi = new OpenAPIExt();

    Info info = new Info();
    String appname = appname(classname);
    info.setTitle(appname + " API");
    info.setDescription(appname + " API description");
    info.setVersion("1.0");
    openapi.info(info);

    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    Paths paths = new Paths();
    for (OperationExt operation : operations) {
      PathItem pathItem = paths.computeIfAbsent(operation.getPattern(), pattern -> new PathItem());
      pathItem.operation(PathItem.HttpMethod.valueOf(operation.getMethod()), operation);
    }
    openapi.setOperations(operations);
    openapi.setPaths(paths);

    return openapi;
  }

  public String toYaml(OpenAPI openAPI) {
    try {
      return Yaml.mapper().writeValueAsString(openAPI);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public String toJson(OpenAPI openAPI) {
    try {
      return Json.mapper().writeValueAsString(openAPI);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public void setDebug(Set<DebugOption> debug) {
    this.debug = debug;
  }

  private String appname(String classname) {
    int i = classname.lastIndexOf('.');
    if (i > 0) {
      String name = classname.substring(i + 1);
      return name.replace("App", "").replace("Kt", "");
    }
    return classname;
  }
}
