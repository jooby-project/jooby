package io.jooby.openapi;

import io.jooby.internal.openapi.ClassSource;
import io.jooby.internal.openapi.DebugOption;
import io.jooby.internal.openapi.ExecutionContext;
import io.jooby.internal.openapi.Operation;
import io.jooby.internal.openapi.OperationParser;
import io.jooby.internal.openapi.TypeFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class OpenApiTool {

  private Path basedir = Paths.get(System.getProperty("user.dir"));

  private Path targetDir = basedir.resolve("target").resolve("classes");

  private Set<DebugOption> debug;

  public List<Operation> process(String classname) {
    ClassSource source = new ClassSource(targetDir);

    OperationParser routes = new OperationParser();
    ExecutionContext ctx = new ExecutionContext(source, TypeFactory.fromJavaName(classname), debug);
    return routes.parse(ctx);
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
