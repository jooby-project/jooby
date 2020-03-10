/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.ClassSource;
import io.jooby.internal.openapi.ContextPathParser;
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
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OpenAPIGenerator {

  private Set<DebugOption> debug;

  private ClassLoader classLoader;

  private Path basedir = java.nio.file.Paths.get(System.getProperty("user.dir"));

  private String templateName = "openapi.yaml";

  public OpenAPI generate(String classname) {
    ClassLoader classLoader = Optional.ofNullable(this.classLoader)
        .orElseGet(getClass()::getClassLoader);
    ClassSource source = new ClassSource(classLoader);

    RouteParser routes = new RouteParser();
    ParserContext ctx = new ParserContext(source, TypeFactory.fromJavaName(classname), debug);
    List<OperationExt> operations = routes.parse(ctx);

    String contextPath = ContextPathParser.parse(ctx);

    /** Create OpenAPI from template and make sure min required information is present: */
    OpenAPIExt openapi = OpenAPIExt.create(basedir, classLoader, templateName);

    defaults(classname, contextPath, openapi);

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

  private void defaults(String classname, String contextPath, OpenAPIExt openapi) {
    Info info = openapi.getInfo();
    if (info == null) {
      info = new Info();
      openapi.info(info);
    }
    String appname = appname(classname);
    info.setTitle(Optional.ofNullable(info.getTitle()).orElse(appname + " API"));
    info.setDescription(
        Optional.ofNullable(info.getDescription()).orElse(appname + " API description"));
    info.setVersion(Optional.ofNullable(info.getVersion()).orElse("1.0"));

    if (openapi.getServers() == null || openapi.getServers().isEmpty()) {
      if (!contextPath.equals("/")) {
        Server server = new Server();
        server.setUrl(contextPath);
        openapi.setServers(Collections.singletonList(server));
      }
    }
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
      return Json.mapper().writer().withDefaultPrettyPrinter().writeValueAsString(openAPI);
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

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public void setBasedir(Path basedir) {
    this.basedir = basedir;
  }

  public Path getBasedir() {
    return basedir;
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
