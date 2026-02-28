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
import io.jooby.internal.openapi.asciidoc.AsciiDocContext;
import io.jooby.internal.openapi.javadoc.JavaDocParser;
import io.jooby.internal.openapi.projection.ProjectionParser;
import io.swagger.v3.core.util.*;
import io.swagger.v3.oas.models.*;
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
      @NonNull protected String toString(
          @NonNull OpenAPIGenerator tool,
          @NonNull OpenAPI result,
          @NonNull Map<String, Object> options) {
        return tool.toJson(result);
      }
    },

    /** YAML. */
    YAML {
      @Override
      @NonNull protected String toString(
          @NonNull OpenAPIGenerator tool,
          @NonNull OpenAPI result,
          @NonNull Map<String, Object> options) {
        return tool.toYaml(result);
      }
    },

    ADOC {
      @Override
      @NonNull protected String toString(
          @NonNull OpenAPIGenerator tool,
          @NonNull OpenAPI result,
          @NonNull Map<String, Object> options) {
        return tool.toAdoc(result, options);
      }

      @SuppressWarnings("unchecked")
      @NonNull @Override
      public List<Path> write(
          @NonNull OpenAPIGenerator tool,
          @NonNull OpenAPI result,
          @NonNull Map<String, Object> options)
          throws IOException {
        var files = (List<Path>) options.get("adoc");
        if (files == null || files.isEmpty()) {
          // adoc generation is optional
          return List.of();
        }
        var outputDir = (Path) options.get("outputDir");
        var outputList = new ArrayList<Path>();
        var context = tool.createAsciidoc(files.getFirst().getParent(), (OpenAPIExt) result);
        for (var file : files) {
          var opts = new HashMap<>(options);
          opts.put("adoc", file);
          var content = toString(tool, result, opts);
          var output = outputDir.resolve(file.getFileName());
          Files.write(output, List.of(content));
          context.export(output, outputDir);
          outputList.add(output);
        }
        return outputList;
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
    protected abstract @NonNull String toString(
        @NonNull OpenAPIGenerator tool,
        @NonNull OpenAPI result,
        @NonNull Map<String, Object> options);

    /**
     * Convert an {@link OpenAPI} model to the current format.
     *
     * @param tool Generator.
     * @param result Model.
     * @return String (json or yaml content).
     */
    public @NonNull List<Path> write(
        @NonNull OpenAPIGenerator tool,
        @NonNull OpenAPI result,
        @NonNull Map<String, Object> options)
        throws IOException {
      var output = (Path) options.get("output");
      var content = toString(tool, result, options);
      Files.write(output, List.of(content));
      return List.of(output);
    }
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

  private boolean javadoc = true;

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
  public @NonNull List<Path> export(
      @NonNull OpenAPI openAPI, @NonNull Format format, @NonNull Map<String, Object> options)
      throws IOException {
    Path output;
    if (openAPI instanceof OpenAPIExt) {
      var source = ((OpenAPIExt) openAPI).getSource();
      var names = source.split("\\.");
      output =
          Stream.of(names).limit(names.length - 1).reduce(outputDir, Path::resolve, Path::resolve);
      var appname = names[names.length - 1];
      if (appname.endsWith("Kt")) {
        appname = appname.substring(0, appname.length() - 2);
      }
      output = output.resolve(appname + "." + format.extension());
    } else {
      throw new ClassCastException(openAPI.getClass() + " is not a " + OpenAPIExt.class);
    }

    if (!Files.exists(output.getParent())) {
      Files.createDirectories(output.getParent());
    }
    var allOptions = new HashMap<>(options);
    allOptions.put("output", output);
    allOptions.put("outputDir", output.getParent());
    return format.write(this, openAPI, allOptions);
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
    var classLoader = Optional.ofNullable(this.classLoader).orElseGet(getClass()::getClassLoader);

    var source = new ClassSource(classLoader);

    /* Create OpenAPI from template and make sure min required information is present: */
    var openapi = new OpenApiTemplate(specVersion).fromTemplate(basedir, classLoader, templateName);

    var mainType = TypeFactory.fromJavaName(classname);
    var javadoc = this.javadoc ? new JavaDocParser(sources) : JavaDocParser.NOOP;

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
                doc.getTags().forEach(openapi::addTagsItem);
              });
    }

    var routes = new RouteParser();
    var json = jsonMapper();
    var yaml = yamlMapper();
    var ctx = new ParserContext(specVersion, json, yaml, source, mainType, javadoc, debug);
    var operations = routes.parse(ctx, openapi);

    var contextPath = ContextPathParser.parse(ctx);

    openapi.setSource(Optional.ofNullable(ctx.getMainClass()).orElse(classname));

    /* Top Level annotations. */
    OpenAPIParser.parse(ctx, openapi);

    defaults(classname, contextPath, openapi);

    Map<String, Tag> globalTags = new LinkedHashMap<>();
    var paths = new Paths();
    for (var operation : operations) {
      var pattern = operation.getPath();
      if (!includes(pattern) || excludes(pattern)) {
        log.debug("skipping {}", pattern);
        continue;
      }
      var regexMap = new HashMap<String, String>();
      Router.pathKeys(
          pattern, (key, value) -> Optional.ofNullable(value).ifPresent(v -> regexMap.put(key, v)));
      if (!regexMap.isEmpty()) {
        for (var e : regexMap.entrySet()) {
          var name = e.getKey();
          var regex = e.getValue();
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
      var pathItem = paths.computeIfAbsent(pattern, k -> new PathItem());
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

    // Put schemas so far
    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    ProjectionParser.parse(ctx, openapi);

    // Save schemas after projection in case a new one was created
    ctx.schemas().forEach(schema -> openapi.schema(schema.getName(), schema));

    finish(openapi);
    return openapi;
  }

  private void finish(OpenAPIExt openapi) {}

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
    var info = openapi.getInfo();
    if (info == null) {
      info = new Info();
      openapi.info(info);
    }
    var appname = appname(classname);
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
   * Generates an adoc version of the given model.
   *
   * @param openAPI Model.
   * @return YAML content.
   */
  public @NonNull String toAdoc(@NonNull OpenAPI openAPI, @NonNull Map<String, Object> options) {
    try {
      var file = (Path) options.get("adoc");
      if (file == null) {
        throw new IllegalArgumentException("'adoc' file is required: " + options);
      }
      return createAsciidoc(file.getParent(), (OpenAPIExt) openAPI).generate(file);
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
   * Set output directory used by {@link #export(OpenAPI, Format, Map)} operation.
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
   * Set output directory used by {@link #export(OpenAPI, Format, Map)}.
   *
   * @param outputDir Output directory.
   */
  public void setOutputDir(@NonNull Path outputDir) {
    this.outputDir = outputDir;
  }

  /**
   * Set the desired spec output. Default is <code>3.0</code>.
   *
   * @param specVersion One of <code>3.0</code> or <code>3.0</code>.
   */
  private void setSpecVersion(SpecVersion specVersion) {
    this.specVersion = specVersion;
  }

  /**
   * Set the desired spec output. Default is <code>3.1</code>.
   *
   * @param version One of <code>3.0</code> or <code>3.1</code>.
   */
  public void setSpecVersion(String version) {
    switch (version) {
      case "v3.1", "v3.1.0", "3.1", "3.1.0", "V31":
        setSpecVersion(SpecVersion.V31);
        break;
      case "v3.0", "v3.0.0", "3.0", "3.0.0", "v3.0.1", "3.0.1", "V30":
        setSpecVersion(SpecVersion.V30);
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid spec version: " + version + ". Supported version: [3.0.1, 3.1.0]");
    }
  }

  /**
   * True/On to enabled.
   *
   * @param javadoc True/On to enabled.
   */
  public void setJavadoc(String javadoc) {
    this.javadoc = Boolean.parseBoolean(javadoc) || "on".equalsIgnoreCase(javadoc);
  }

  /**
   * True/On to enabled.
   *
   * @return True/On to enabled.
   */
  public boolean getJavadoc() {
    return javadoc;
  }

  private AsciiDocContext createAsciidoc(Path basedir, OpenAPIExt openapi) {
    return new AsciiDocContext(basedir, jsonMapper(), yamlMapper(), openapi);
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
