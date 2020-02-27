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
import io.swagger.v3.oas.models.info.Info;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class OpenApiTool {

  private Path basedir = Paths.get(System.getProperty("user.dir"));

  private Path targetDir = basedir.resolve("target").resolve("classes");

  private Set<DebugOption> debug;

  public OpenAPI generate(String classname) {
    ClassSource source = new ClassSource(targetDir);

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

    io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
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

  public void setBasedir(Path basedir) {
    this.basedir = basedir;
  }

  public void setTargetDir(Path targetDir) {
    this.targetDir = targetDir;
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
