/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import io.jooby.Router;
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
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generate an {@link OpenAPI} model from a Jooby application.
 *
 * Optionally exports an {@link OpenAPI} model to a json or yaml file.
 *
 * Usage: https://jooby.io/modules/openapi
 *
 * @author edgar
 */
public class OpenAPIGenerator {

  /**
   * Supported formats.
   */
  public enum Format {
    /**
     * JSON.
     */
    JSON {
      @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toJson(result);
      }
    },

    /**
     * YAML.
     */
    YAML {
      @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toYaml(result);
      }
    };

    /**
     * File extension.
     *
     * @return File extension.
     */
    public @Nonnull String extension() {
      return name().toLowerCase();
    }

    /**
     * Convert an {@link OpenAPI} model to the current format.
     *
     * @param tool Generator.
     * @param result Model.
     * @return String (json or yaml content).
     */
    public abstract @Nonnull String toString(@Nonnull OpenAPIGenerator tool,
        @Nonnull OpenAPI result);

  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private Set<DebugOption> debug;

  private ClassLoader classLoader;

  private Path basedir = java.nio.file.Paths.get(System.getProperty("user.dir"));

  private Path outputDir = basedir;

  private String templateName = "openapi.yaml";

  private String includes;

  private String excludes;

  /**
   * Export an {@link OpenAPI} model to the given format.
   *
   * @param openAPI Model.
   * @param format Format.
   * @throws IOException
   * @return Output file.
   */
  public @Nonnull Path export(@Nonnull OpenAPI openAPI, @Nonnull Format format) throws IOException {
    Path output;
    if (openAPI instanceof OpenAPIExt) {
      String source = ((OpenAPIExt) openAPI).getSource();
      String[] names = source.split("\\.");
      output = Stream.of(names).limit(names.length - 1)
          .reduce(outputDir, Path::resolve, Path::resolve);
      String appname = names[names.length - 1];
      if (appname.endsWith("Kt")) {
        appname = appname.substring(0, appname.length() - 2);
      }
      output = output.resolve(appname + "." + format.extension());
    } else {
      output = outputDir.resolve("openapi." + format.extension());
    }

    if (!Files.exists(output.getParent())) {
      Files.createDirectories(output.getParent());
    }

    String content = format.toString(this, openAPI);
    Files.write(output, Collections.singleton(content));
    return output;
  }

  /**
   * Generate an {@link OpenAPI} model from Jooby class. This method parses class byte code and
   * generates an open api model from it. Compilation must be done with debug information and
   * parameters name available.
   *
   * Optionally, the <code>conf/openapi.yaml</code> is used as template and get merged into the
   * final model.
   *
   * @param classname Application class name.
   * @return Model.
   */
  public @Nonnull OpenAPI generate(@Nonnull String classname) {
    ClassLoader classLoader = Optional.ofNullable(this.classLoader)
        .orElseGet(getClass()::getClassLoader);
    ClassSource source = new ClassSource(classLoader);

    RouteParser routes = new RouteParser();
    ParserContext ctx = new ParserContext(source, TypeFactory.fromJavaName(classname), debug);
    List<OperationExt> operations = routes.parse(ctx);

    String contextPath = ContextPathParser.parse(ctx);

    /** Create OpenAPI from template and make sure min required information is present: */
    OpenAPIExt openapi = OpenAPIExt.create(basedir, classLoader, templateName);
    openapi.setSource(Optional.ofNullable(ctx.getMainClass()).orElse(classname));

    defaults(classname, contextPath, openapi);

    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    Map<String, Tag> globalTags = new LinkedHashMap<>();
    Paths paths = new Paths();
    for (OperationExt operation : operations) {
      String pattern = operation.getPattern();
      if (!includes(pattern) || excludes(pattern)) {
        log.debug("skipping {}", pattern);
        continue;
      }
      Map<String, String> regexMap = new HashMap<>();
      Router.pathKeys(pattern, (key, value) -> Optional.ofNullable(value)
          .ifPresent(v -> regexMap.put(key, v)));
      if (regexMap.size() > 0) {
        for (Map.Entry<String, String> e : regexMap.entrySet()) {
          String name = e.getKey();
          String regex = e.getValue();
          operation.getParameter(name).ifPresent(parameter ->
              parameter.getSchema().setPattern(regex)
          );
          if (regex.equals("\\.*")) {
            if (name.equals("*")) {
              pattern = pattern.substring(0, pattern.length() - 1) + "{*}";
            } else {
              pattern = pattern.replace("*" + name, "{" + name + "}");
            }
          } else {
            pattern = pattern.replace(name + ":" + regex, name);
          }
        }
      }
      PathItem pathItem = paths.computeIfAbsent(pattern, k -> new PathItem());
      pathItem.operation(PathItem.HttpMethod.valueOf(operation.getMethod()), operation);
      Optional.ofNullable(operation.getPathSummary()).ifPresent(pathItem::setSummary);
      Optional.ofNullable(operation.getPathDescription()).ifPresent(pathItem::setDescription);

      // global tags
      operation.getGlobalTags().forEach(tag -> globalTags.put(tag.getName(), tag));
    }
    globalTags.values().forEach(tag -> {
      if (tag.getDescription() != null) {
        openapi.addTagsItem(tag);
      }
    });
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

  /**
   * Generates a YAML version of the given model.
   *
   * @param openAPI Model.
   * @return YAML content.
   */
  public @Nonnull String toYaml(@Nonnull OpenAPI openAPI) {
    try {
      return Yaml.mapper().writeValueAsString(openAPI);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Generates a JSON version of the given model.
   *
   * @param openAPI Model.
   * @return JSON content.
   */
  public @Nonnull String toJson(@Nonnull OpenAPI openAPI) {
    try {
      return Json.mapper().writer().withDefaultPrettyPrinter().writeValueAsString(openAPI);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Use a custom classloader for resolving class files.
   *
   * @param classLoader Class loader.
   */
  public void setClassLoader(@Nonnull ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Set debug options.
   *
   * @param debug Debug options.
   */
  public void setDebug(final Set<DebugOption> debug) {
    this.debug = debug;
  }

  /**
   * OpenAPI template file name, defaults is: <code>openapi.yaml</code>.
   *
   * @return OpenAPI template file name, defaults is: <code>openapi.yaml</code>.
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * Set openAPI template file name, defaults is: <code>openapi.yaml</code>.
   *
   * @param templateName OpenAPI template file name, defaults is: <code>openapi.yaml</code>.
   */
  public void setTemplateName(@Nonnull String templateName) {
    this.templateName = templateName;
  }

  /**
   * Set base directory used it for loading openAPI template file name.
   * Defaults is <code>user.dir</code>.
   *
   * @param basedir Base directory.
   */
  public void setBasedir(@Nonnull Path basedir) {
    this.basedir = basedir;
  }

  /**
   * Base directory used it for loading openAPI template file name.
   *
   * Defaults is <code>user.dir</code>.
   *
   * @return Base directory used it for loading openAPI template file name.
   */
  public Path getBasedir() {
    return basedir;
  }

  /**
   * Set output directory used by {@link #export(OpenAPI, Format)} operation.
   *
   * Defaults to {@link #getBasedir()}.
   *
   * @return Get output directory.
   */
  public Path getOutputDir() {
    return outputDir;
  }

  /**
   * Regular expression used to includes/keep route. Example: <code>/api/.*</code>.
   *
   * @return Regular expression used to includes/keep route. Example: <code>/api/.*</code>.
   */
  public @Nullable String getIncludes() {
    return includes;
  }

  /**
   * Set regular expression used to includes/keep route. Example: <code>/api/.*</code>.
   *
   * @param includes Regular expression.
   */
  public void setIncludes(@Nullable String includes) {
    this.includes = includes;
  }

  /**
   * Regular expression used to excludes route. Example: <code>/web</code>.
   *
   * @return Regular expression used to excludes route. Example: <code>/web</code>.
   */
  public @Nullable String getExcludes() {
    return excludes;
  }

  /**
   * Set Regular expression used to excludes route. Example: <code>/web</code>.
   *
   * @param excludes Regular expression used to excludes route. Example: <code>/web</code>.
   */
  public void setExcludes(@Nullable String excludes) {
    this.excludes = excludes;
  }

  /**
   * Set output directory used by {@link #export(OpenAPI, Format)}.
   *
   * @param outputDir Output directory.
   */
  public void setOutputDir(@Nonnull Path outputDir) {
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
