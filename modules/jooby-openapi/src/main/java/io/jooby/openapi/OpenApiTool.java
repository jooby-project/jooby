package io.jooby.openapi;

import io.jooby.internal.openapi.ClassSource;
import io.jooby.internal.openapi.ExecutionContext;
import io.jooby.internal.openapi.RouteDescriptor;
import io.jooby.internal.openapi.RouteReader;
import io.jooby.internal.openapi.TypeFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class OpenApiTool {

  private Path basedir = Paths.get(System.getProperty("user.dir"));

  private Path targetDir = basedir.resolve("target").resolve("classes");

  private boolean debug;

  public List<RouteDescriptor> process(String classname) {
    ClassSource source = new ClassSource(targetDir);

    RouteReader routes = new RouteReader();
    ExecutionContext ctx = new ExecutionContext(source, TypeFactory.fromJavaName(classname), debug);
    return routes.routes(ctx);
  }

  public void setBasedir(Path basedir) {
    this.basedir = basedir;
  }

  public void setTargetDir(Path targetDir) {
    this.targetDir = targetDir;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }
}
