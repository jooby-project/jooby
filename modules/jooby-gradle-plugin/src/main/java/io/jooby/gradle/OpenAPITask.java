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

import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

  /**
   * Generate OpenAPI files from Jooby application.
   *
   * @throws Throwable If something goes wrong.
   */
  @TaskAction
  public void generate() throws Throwable {
    List<Project> projects = getProjects();

    String mainClass = Optional.ofNullable(this.mainClass)
        .orElseGet(() -> computeMainClassName(projects));
    var sources = projects.stream()
        .flatMap(project -> {
          var sourceSet = sourceSet(project, false);
          return sourceSet.stream()
              .flatMap(it -> it.getAllSource().getSrcDirs().stream())
              .map(File::toPath);
        })
        .distinct()
        .toList();    Path outputDir = classes(getProject(), false);

    ClassLoader classLoader = createClassLoader(projects);

    getLogger().info("Generating OpenAPI: " + mainClass);
    getLogger().debug("Using classloader: " + classLoader);
    getLogger().debug("Output directory: " + outputDir);
    getLogger().debug("Source directories: " + sources);

    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setClassLoader(classLoader);
    tool.setOutputDir(outputDir);
    tool.setSources(sources);
    trim(includes).ifPresent(tool::setIncludes);
    trim(excludes).ifPresent(tool::setExcludes);

    OpenAPI result = tool.generate(mainClass);

    for (OpenAPIGenerator.Format format : OpenAPIGenerator.Format.values()) {
      Path output = tool.export(result, format);
      getLogger().info("  writing: " + output);
    }
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

  private Optional<String> trim(String value) {
    if (value == null || value.trim().length() == 0) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }
}
