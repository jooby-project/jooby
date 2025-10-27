/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Router;
import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.*;
import io.jooby.internal.openapi.javadoc.JavaDocParser;
import io.swagger.v3.core.util.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Generate an {@link OpenAPI} model from a Jooby application.
 *
 * <p>Optionally exports an {@link OpenAPI} model to a json or yaml file.
 *
 * <p>Usage: https://jooby.io/modules/openapi
 *
 * @author edgar
 */
public class OpenAPIGenerator {

  /** Supported formats. */
  public enum Format {
    /** JSON. */
    JSON {
      @Override
      public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toJson(result);
      }
    },

    /** YAML. */
    YAML {
      @Override
      public String toString(OpenAPIGenerator tool, OpenAPI result) {
        return tool.toYaml(result);
      }
    };

    /**
     * File extension.
     *
     * @return File extension.
     */
    public @NonNull String extension() {
      return name().toLowerCase();
    }

    /**
     * Convert an {@link OpenAPI} model to the current format.
     *
     * @param tool Generator.
     * @param result Model.
     * @return String (json or yaml content).
     */
    public abstract @NonNull String toString(
        @NonNull OpenAPIGenerator tool, @NonNull OpenAPI result);
  }

  private Logger log = LoggerFactory.getLogger(getClass());

  private Set<DebugOption> debug;

  private ClassLoader classLoader;

  private Path basedir = java.nio.file.Paths.get(System.getProperty("user.dir"));

  private Path outputDir = basedir;

  private String templateName = "openapi.yaml";

  private String includes;

  private String excludes;

  private List<Path> sources;

  private SpecVersion specVersion = SpecVersion.V30;

  /** Default constructor. */
  public OpenAPIGenerator() {}

  /**
   * Export an {@link OpenAPI} model to the given format.
   *
   * @param openAPI Model.
   * @param format Format.
   * @return Output file.
   * @throws IOException If fails to process input.
   */
  public @NonNull Path export(@NonNull OpenAPI openAPI, @NonNull Format format) throws IOException {
    Path output;
    if (openAPI instanceof OpenAPIExt) {
      String source = ((OpenAPIExt) openAPI).getSource();
      String[] names = source.split("\\.");
      output =
          Stream.of(names).limit(names.length - 1).reduce(outputDir, Path::resolve, Path::resolve);
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
   * <p>Optionally, the <code>conf/openapi.yaml</code> is used as template and get merged into the
   * final model.
   *
   * @param classname Application class name.
   * @return Model.
   */
  public @NonNull OpenAPI generate(@NonNull String classname) {
    ClassLoader classLoader =
        Optional.ofNullable(this.classLoader).orElseGet(getClass()::getClassLoader);

    ClassSource source = new ClassSource(classLoader);

    /* Create OpenAPI from template and make sure min required information is present: */
    OpenAPIExt openapi =
        new OpenApiTemplate(specVersion).fromTemplate(basedir, classLoader, templateName);

    var mainType = TypeFactory.fromJavaName(classname);
    var javadoc = new JavaDocParser(sources);

    if (openapi.getInfo() == null) {
      var info = new Info();
      openapi.setInfo(info);
      javadoc
          .parse(classname)
          .ifPresent(
              doc -> {
                Optional.ofNullable(doc.getSummary()).ifPresent(info::setTitle);
                Optional.ofNullable(doc.getDescription()).ifPresent(info::setDescription);
                Optional.ofNullable(doc.getVersion()).ifPresent(info::setVersion);
                if (!doc.getExtensions().isEmpty()) {
                  info.setExtensions(doc.getExtensions());
                }
                doc.getSecuritySchemes().forEach(openapi::addSecuritySchemes);
                doc.getServers().forEach(openapi::addServersItem);
                doc.getContact().forEach(info::setContact);
                doc.getLicense().forEach(info::setLicense);
              });
    }

    RouteParser routes = new RouteParser();
    var json = jsonMapper();
    var yaml = yamlMapper();
    ParserContext ctx =
        new ParserContext(specVersion, json, yaml, source, mainType, javadoc, debug);
    List<OperationExt> operations = routes.parse(ctx, openapi);

    String contextPath = ContextPathParser.parse(ctx);

    openapi.setSource(Optional.ofNullable(ctx.getMainClass()).orElse(classname));

    /* Top Level annotations. */
    OpenAPIParser.parse(ctx, openapi);

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
      Router.pathKeys(
          pattern, (key, value) -> Optional.ofNullable(value).ifPresent(v -> regexMap.put(key, v)));
      if (!regexMap.isEmpty()) {
        for (Map.Entry<String, String> e : regexMap.entrySet()) {
          String name = e.getKey();
          String regex = e.getValue();
          operation
              .getParameter(name)
              .ifPresent(parameter -> parameter.getSchema().setPattern(regex));
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
      Optional.ofNullable(operation.getPathExtensions()).ifPresent(pathItem::setExtensions);

      // global tags
      operation
          .getGlobalTags()
          .forEach(
              tag -> {
                if (tag.getDescription() != null || tag.getExtensions() != null) {
                  globalTags.put(tag.getName(), tag);
                }
              });
    }
    globalTags.values().forEach(openapi::addTagsItem);
    openapi.setOperations(operations);
    openapi.setPaths(paths);

    if (SpecVersion.V31 == openapi.getSpecVersion()) {
      new OpenAPI30To31().process(openapi);
      openapi.setJsonSchemaDialect(null);
    }

    return openapi;
  }

  ObjectMapper yamlMapper() {
    return specVersion == SpecVersion.V30 ? Yaml.mapper() : Yaml31.mapper();
  }

  ObjectMapper jsonMapper() {
    return specVersion == SpecVersion.V30 ? Json.mapper() : Json31.mapper();
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
  public @NonNull String toYaml(@NonNull OpenAPI openAPI) {
    try {
      return yamlMapper().writeValueAsString(openAPI);
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
  public @NonNull String toJson(@NonNull OpenAPI openAPI) {
    try {
      return jsonMapper().writer().withDefaultPrettyPrinter().writeValueAsString(openAPI);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Use a custom classloader for resolving class files.
   *
   * @param classLoader Class loader.
   */
  public void setClassLoader(@NonNull ClassLoader classLoader) {
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
  public void setTemplateName(@NonNull String templateName) {
    this.templateName = templateName;
  }

  /**
   * Set base directory used it for loading openAPI template file name. Defaults is <code>user.dir
   * </code>.
   *
   * @param basedir Base directory.
   */
  public void setBasedir(@NonNull Path basedir) {
    this.basedir = basedir;
  }

  /**
   * Where to find source code. Required for javadoc parsing.
   *
   * @param sources Source code location.
   */
  public void setSources(@NonNull List<Path> sources) {
    this.sources = sources;
  }

  /**
   * Base directory used it for loading openAPI template file name.
   *
   * <p>Defaults is <code>user.dir</code>.
   *
   * @return Base directory used it for loading openAPI template file name.
   */
  public Path getBasedir() {
    return basedir;
  }

  /**
   * Set output directory used by {@link #export(OpenAPI, Format)} operation.
   *
   * <p>Defaults to {@link #getBasedir()}.
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
  public void setOutputDir(@NonNull Path outputDir) {
    this.outputDir = outputDir;
  }

  /**
   * Set the desired spec output. Default is <code>3.1</code>.
   *
   * @param specVersion One of <code>3.0</code> or <code>3.1</code>.
   */
  public void setSpecVersion(SpecVersion specVersion) {
    this.specVersion = specVersion;
  }

  /**
   * Set the desired spec output. Default is <code>3.1</code>.
   *
   * @param version One of <code>3.0</code> or <code>3.1</code>.
   */
  public void setSpecVersion(String version) {
    if (specVersion != null) {
      switch (version) {
        case "v3.1", "v3.1.0", "3.1", "3.1.0":
          setSpecVersion(SpecVersion.V31);
        case "v3.0", "v3.0.0", "3.0", "3.0.0", "v3.0.1", "3.0.1":
          setSpecVersion(SpecVersion.V30);
        default:
          throw new IllegalArgumentException(
              "Invalid spec version: " + version + ". Supported version: [3.0.1, 3.1.0]");
      }
    }
  }

  private String appname(String classname) {
    String name = classname;
    int i = name.lastIndexOf('.');
    if (i > 0) {
      name = name.substring(i + 1);
      name = name.replace("App", "").replace("Kt", "").trim();
    }
    return name.isEmpty() ? "My App" : name;
  }
}
