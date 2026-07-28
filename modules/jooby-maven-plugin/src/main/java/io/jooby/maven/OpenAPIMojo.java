/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static java.util.Optional.ofNullable;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jspecify.annotations.Nullable;

import io.jooby.openapi.OpenAPIGenerator;

/**
 * Generate an OpenAPI file from a jooby application.
 *
 * <p>Usage: https://jooby.io/modules/openapi
 *
 * @author edgar
 * @since 2.7.0
 */
@Mojo(
    name = "openapi",
    threadSafe = true,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
    aggregator = true,
    defaultPhase = PROCESS_CLASSES)
public class OpenAPIMojo extends BaseMojo {

  @Parameter(property = "openAPI.includes")
  private String includes;

  @Parameter(property = "openAPI.excludes")
  private String excludes;

  @Parameter(property = "openAPI.specVersion")
  private String specVersion;

  @Parameter(property = "openAPI.javadoc")
  private String javadoc;

  @Parameter private List<File> adoc;

  @Parameter(property = "openAPI.copyOpenApiSpecTo")
  private File copyOpenApiSpecTo;

  @Override
  protected void doExecute(List<MavenProject> projects, String mainClass) throws Exception {
    ClassLoader classLoader = createClassLoader(projects);
    Path outputDir = Paths.get(project.getBuild().getOutputDirectory());
    var sources =
        projects.stream()
            .map(project -> Paths.get(project.getBuild().getSourceDirectory()))
            .distinct()
            .toList();

    getLog().info("Generating OpenAPI: " + mainClass);
    getLog().debug("Using classloader: " + classLoader);
    getLog().debug("Output directory: " + outputDir);
    getLog().debug("Source directories: " + sources);

    OpenAPIGenerator tool = new OpenAPIGenerator();
    if (specVersion != null) {
      tool.setSpecVersion(specVersion);
    }
    tool.setClassLoader(classLoader);
    tool.setOutputDir(outputDir);
    tool.setSources(sources);
    trim(includes).ifPresent(tool::setIncludes);
    trim(excludes).ifPresent(tool::setExcludes);
    if (javadoc != null && !javadoc.trim().isEmpty()) {
      tool.setJavadoc(javadoc.trim());
    }

    var result = tool.generate(mainClass);

    var adocPath = ofNullable(adoc).orElse(List.of()).stream().map(File::toPath).toList();
    var written = new ArrayList<Path>();
    for (var format : OpenAPIGenerator.Format.values()) {
      written.addAll(tool.export(result, format, Map.of("adoc", adocPath)));
    }
    written.forEach(output -> getLog().info("  writing: " + output));

    if (copyOpenApiSpecTo != null) {
      var destination = copyOpenApiSpecTo.toPath();
      copySpec(written, destination);
      getLog().info("  copying: " + destination);
    }
  }

  public static void copySpec(List<Path> written, Path destination) throws IOException {
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

  public static OpenAPIGenerator.Format specFormat(Path destination) {
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

  private Optional<String> trim(String value) {
    if (value == null || value.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
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
   * Spec version. Default is <code>3.0</code>.
   *
   * @return Spec version. Default is <code>3.0</code>.
   */
  public String getSpecVersion() {
    return specVersion;
  }

  /**
   * Set the desired spec output. Default is <code>3.0</code>.
   *
   * @param specVersion One of <code>3.0</code> or <code>3.0</code>.
   */
  public void setSpecVersion(String specVersion) {
    this.specVersion = specVersion;
  }

  /**
   * List of asciidoc files to generate documentation.
   *
   * @return List of asciidoc files to generate documentation.
   */
  public List<File> getAdoc() {
    return adoc;
  }

  /**
   * List of asciidoc files to generate documentation.
   *
   * @param adoc List of asciidoc files to generate documentation.
   */
  public void setAdoc(List<File> adoc) {
    this.adoc = adoc;
  }

  /**
   * True/On to enabled. By default is: <code>on</code>.
   *
   * @param javadoc True/On to enabled.
   */
  public void setJavadoc(String javadoc) {
    this.javadoc = javadoc;
  }

  /**
   * True/On to enabled.
   *
   * @return True/On to enabled.
   */
  public String getJavadoc() {
    return javadoc;
  }

  /**
   * Copy the generated OpenAPI spec to the given file. The format is determined by the file
   * extension: <code>.yaml</code>, <code>.yml</code> or <code>.json</code>.
   *
   * @return Destination file.
   */
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
   * <copyOpenApiSpecTo>${project.basedir}/docs/openapi.yaml</copyOpenApiSpecTo>
   * }</pre>
   *
   * @param copyOpenApiSpecTo Destination file.
   */
  public void setCopyOpenApiSpecTo(@Nullable File copyOpenApiSpecTo) {
    this.copyOpenApiSpecTo = copyOpenApiSpecTo;
  }
}
