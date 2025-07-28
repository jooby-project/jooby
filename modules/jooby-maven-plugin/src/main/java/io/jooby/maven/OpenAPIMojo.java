/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.OpenAPI;

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

  @Override
  protected void doExecute(@NonNull List<MavenProject> projects, @NonNull String mainClass)
      throws Exception {
    ClassLoader classLoader = createClassLoader(projects);
    Path outputDir = Paths.get(project.getBuild().getOutputDirectory());
    // Reduce lookup to current project: See https://github.com/jooby-project/jooby/issues/2756

    getLog().info("Generating OpenAPI: " + mainClass);
    getLog().debug("Using classloader: " + classLoader);
    getLog().debug("Output directory: " + outputDir);

    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setClassLoader(classLoader);
    tool.setOutputDir(outputDir);
    trim(includes).ifPresent(tool::setIncludes);
    trim(excludes).ifPresent(tool::setExcludes);

    OpenAPI result = tool.generate(mainClass);

    for (OpenAPIGenerator.Format format : OpenAPIGenerator.Format.values()) {
      Path output = tool.export(result, format);
      getLog().info("  writing: " + output);
    }
  }

  private Optional<String> trim(String value) {
    if (value == null || value.trim().length() == 0) {
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
}
