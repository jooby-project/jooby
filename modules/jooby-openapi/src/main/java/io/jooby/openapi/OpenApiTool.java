package io.jooby.openapi;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.ClassSource;
import io.jooby.internal.openapi.DebugOption;
import io.jooby.internal.openapi.ExecutionContext;
import io.jooby.internal.openapi.Operation;
import io.jooby.internal.openapi.OperationParser;
import io.jooby.internal.openapi.TypeFactory;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class OpenApiTool {

  private Path basedir = Paths.get(System.getProperty("user.dir"));

  private Path targetDir = basedir.resolve("target").resolve("classes");

  private Set<DebugOption> debug;

  public OpenAPI process(String classname, Consumer<Operation> consumer) {
    ClassSource source = new ClassSource(targetDir);

    OperationParser routes = new OperationParser();
    ExecutionContext ctx = new ExecutionContext(source, TypeFactory.fromJavaName(classname), debug);
    List<Operation> operations = routes.parse(ctx);

    OpenAPI openapi = new OpenAPI();

    Info info = new Info();
    String appname = appname(classname);
    info.setTitle(appname + " API");
    info.setDescription(appname + " API description");
    info.setVersion("1.0");
    openapi.info(info);

    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
    for (Operation operation : operations) {
      consumer.accept(operation);
      PathItem pathItem = paths.computeIfAbsent(operation.getPattern(), pattern -> new PathItem());
      pathItem.operation(PathItem.HttpMethod.valueOf(operation.getMethod()), operation);
    }

    openapi.setPaths(paths);

    return openapi;
  }

  private String appname(String classname) {
    int i = classname.lastIndexOf('.');
    if (i > 0) {
      String name = classname.substring(i + 1);
      return name.replace("App", "").replace("Kt", "");
    }
    return classname;
  }

  public String toYaml(OpenAPI openAPI) {
    try {
      return Yaml.mapper().writeValueAsString(openAPI);
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
}
