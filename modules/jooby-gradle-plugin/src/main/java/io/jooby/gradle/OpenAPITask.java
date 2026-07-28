/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import org.gradle.api.Project;
import org.gradle.api.model.ReplacedBy;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Generate an OpenAPI file from a jooby application.
 *
 * Usage: https://jooby.io/modules/openapi
 *
 * @author edgar
 * @since 2.7.0
 */
public class OpenAPITask extends BaseTask {

  private String mainClass;

  private String includes;

  private String excludes;

  private String specVersion;

  private List<File> adoc;

  private String javadoc;

  private File copyOpenApiSpecTo;

  /**
   * Creates an OpenAPI task.
   */
  public OpenAPITask() {}

  /**
   * Generate OpenAPI files from Jooby application.
   *
   * @throws Throwable If something goes wrong.
   */
  @TaskAction
  public void generate() throws Throwable {
    List<Project> projects = getProjects();

    String mainClass = ofNullable(this.mainClass)
        .orElseGet(() -> computeMainClassName(projects));
    var sources = projects.stream()
        .flatMap(project -> {
          var sourceSet = sourceSet(project, false);
          return sourceSet.stream()
              .flatMap(it -> it.getAllSource().getSrcDirs().stream())
              .map(File::toPath);
        })
        .distinct()
        .toList();
    Path outputDir = classes(getProject(), false);

    ClassLoader classLoader = createClassLoader(projects);

    getLogger().info("Generating OpenAPI: " + mainClass);
    getLogger().debug("Using classloader: " + classLoader);
    getLogger().debug("Output directory: " + outputDir);
    getLogger().debug("Source directories: " + sources);

    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setClassLoader(classLoader);
    tool.setOutputDir(outputDir);
    tool.setSources(sources);
    if (specVersion != null) {
      tool.setSpecVersion(specVersion);
    }
    trim(includes).ifPresent(tool::setIncludes);
    trim(excludes).ifPresent(tool::setExcludes);
    if (javadoc != null && !javadoc.trim().isEmpty()) {
      tool.setJavadoc(javadoc.trim());
    }

    OpenAPI result = tool.generate(mainClass);

    var adocPath = ofNullable(adoc).orElse(List.of()).stream().map(File::toPath).toList();
    var written = new ArrayList<Path>();
    for (var format : OpenAPIGenerator.Format.values()) {
      written.addAll(tool.export(result, format, Map.of("adoc", adocPath)));
    }
    written.forEach(output -> getLogger().info("  writing: " + output));

    if (copyOpenApiSpecTo != null) {
      var destination = copyOpenApiSpecTo.toPath();
      copySpec(written, destination);
      getLogger().info("  copying: " + destination);
    }
  }

  static void copySpec(List<Path> written, Path destination) throws IOException {
    var format = specFormat(destination);
    var source =
        written.stream()
            .filter(path -> path.getFileName().toString().endsWith("." + format.extension()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IOException(
                        String.format(
                            "OpenAPI %s output not found for copyOpenApiSpecTo: %s",
                            format.name(), destination)));
    var parent = destination.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  static OpenAPIGenerator.Format specFormat(Path destination) {
    var name = destination.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".json")) {
      return OpenAPIGenerator.Format.JSON;
    }
    if (name.endsWith(".yaml") || name.endsWith(".yml")) {
      return OpenAPIGenerator.Format.YAML;
    }
    throw new IllegalArgumentException(
        "copyOpenApiSpecTo must end with .yaml, .yml or .json: " + destination);
  }

  /**
   * Class to parse.
   *
   * @return Class to parse.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set Class to parse.
   * @param mainClassName Class to parse.
   */
  public void setMainClass(String mainClassName) {
    this.mainClass = mainClassName;
  }

  /**
   * Class to parse.
   *
   * @return Class to parse.
   */
  @Deprecated
  @ReplacedBy("mainClass")
  public String getMainClassName() {
    return getMainClass();
  }

  /**
   * Set Class to parse.
   * @param mainClassName Class to parse.
   */
  @Deprecated
  public void setMainClassName(String mainClassName) {
    setMainClass(mainClassName);
  }

  /**
   * Regular expression used to includes/keep route. Example: <code>/api/.*</code>.
   *
   * @return Regular expression used to includes/keep route. Example: <code>/api/.*</code>.
   */
  @Input
  @org.gradle.api.tasks.Optional
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
  @Input
  @org.gradle.api.tasks.Optional
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
   * Get the OpenAPI spec version. One of <code>3.0.1</code>, <code>3.1.0</code>.
   *
   * @return The OpenAPI spec version. One of <code>3.0.1</code>, <code>3.1.0</code>.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public String getSpecVersion() {
    return specVersion;
  }

  /**
   * Set the spec version to use.
   *
   * @param specVersion Spec version. One of <code>3.0.1</code>, <code>3.1.0</code>.
   */
  public void setSpecVersion(String specVersion) {
    this.specVersion = specVersion;
  }

  /**
   * True/On to enabled javadoc output. Default is: <code>on</code>.
   *
   * @return True/On to enabled javadoc output. Default is: <code>on</code>.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public String getJavadoc() {
    return javadoc;
  }


  /**
   * Enabled or disabled javadoc generation. Set to off/false to turn it off.
   * Default is: <code>on</code>.
   *
   * @param javadoc Enabled or disabled javadoc generation. Set to off/false to turn it off.
   *                Default is: <code>on</code>.
   */
  public void setJavadoc(String javadoc) {
    this.javadoc = javadoc;
  }

  /**
   * Optionally generates adoc output.
   *
   * @return List of adoc templates.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public List<File> getAdoc() {
    return adoc;
  }

  /**
   * Set adoc templates to build.
   *
   * @param adoc Adoc templates to build.
   */
  public void setAdoc(List<File> adoc) {
    this.adoc = adoc;
  }

  /**
   * Copy the generated OpenAPI spec to the given file. The format is determined by the file
   * extension: <code>.yaml</code>, <code>.yml</code> or <code>.json</code>.
   *
   * @return Destination file.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public @Nullable File getCopyOpenApiSpecTo() {
    return copyOpenApiSpecTo;
  }

  /**
   * Copy the generated OpenAPI spec to the given file. The format is determined by the file
   * extension: <code>.yaml</code>, <code>.yml</code> or <code>.json</code>.
   *
   * <p>Example:
   *
   * <pre>{@code
   * openAPI {
   *   copyOpenApiSpecTo = file("$projectDir/docs/openapi.yaml")
   * }
   * }</pre>
   *
   * @param copyOpenApiSpecTo Destination file.
   */
  public void setCopyOpenApiSpecTo(@Nullable File copyOpenApiSpecTo) {
    this.copyOpenApiSpecTo = copyOpenApiSpecTo;
  }

  private Optional<String> trim(String value) {
    if (value == null || value.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }
}
