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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAPIGenerator {

  public enum Format {
    JSON {
      @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toJson(result);
      }
    },

    YAML {
      @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toYaml(result);
      }
    };

    public String extension() {
      return name().toLowerCase();
    }

    public static List<Format> parse(String value) {
      if (value == null || value.trim().isEmpty()) {
        return Arrays.asList(JSON, YAML);
      }
      return Stream.of(value.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(String::toUpperCase)
          .map(Format::valueOf)
          .collect(Collectors.toList());
    }

    public abstract String toString(OpenAPIGenerator tool, OpenAPI result);

  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private Set<DebugOption> debug;

  private ClassLoader classLoader;

  private Path basedir = java.nio.file.Paths.get(System.getProperty("user.dir"));

  private Path outputDir = basedir;

  private String templateName = "openapi.yaml";

  private String includes;

  private String excludes;

  public void export(OpenAPI openAPI, Format format) throws IOException {
    Path output;
    if (openAPI instanceof OpenAPIExt) {
      String source = ((OpenAPIExt) openAPI).getSource();
      String[] names = source.split("\\.");
      output = Stream.of(names).limit(Math.max(0, names.length - 2))
          .reduce(outputDir, Path::resolve, Path::resolve);
      output = output.resolve(names[names.length - 1] + "." + format.extension());
    } else {
      output = outputDir.resolve("openapi." + format.extension());
    }

    if (!Files.exists(output.getParent())) {
      Files.createDirectories(output.getParent());
    }

    log.info("  writing: " + output);

    String content = format.toString(this, openAPI);
    Files.write(output, Collections.singleton(content));
  }

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
    openapi.setSource(classname);

    defaults(classname, contextPath, openapi);

    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    Paths paths = new Paths();
    for (OperationExt operation : operations) {
      String pattern = operation.getPattern();
      if (!includes(pattern) || excludes(pattern)) {
        log.debug("skipping {}", pattern);
        continue;
      }
      PathItem pathItem = paths.computeIfAbsent(pattern, k -> new PathItem());
      pathItem.operation(PathItem.HttpMethod.valueOf(operation.getMethod()), operation);
    }
    openapi.setOperations(operations);
    openapi.setPaths(paths);

    return openapi;
  }

  private boolean includes(String value) {
    return pattern(includes, value).orElse(true);
  }

  private boolean excludes(String value) {
    return pattern(excludes, value).orElse(false);
  }

  private Optional<Boolean> pattern(String pattern, String value) {
    return Optional.ofNullable(pattern).map(regex -> Pattern.matches(regex, value));
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

  public Path getOutputDir() {
    return outputDir;
  }

  public String getIncludes() {
    return includes;
  }

  public void setIncludes(String includes) {
    this.includes = includes;
  }

  public String getExcludes() {
    return excludes;
  }

  public void setExcludes(String excludes) {
    this.excludes = excludes;
  }

  public void setOutputDir(Path outputDir) {
    this.outputDir = outputDir;
  }

  private String appname(String classname) {
    String name = classname;
    int i = name.lastIndexOf('.');
    if (i > 0) {
      name = name.substring(i + 1);
      name = name.replace("App", "")
          .replace("Kt", "")
          .trim();
    }
    return name.length() == 0 ? "My App" : name;
  }
}
