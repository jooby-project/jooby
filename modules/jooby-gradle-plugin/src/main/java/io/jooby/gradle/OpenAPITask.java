/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Generate an OpenAPI file from a jooby application.
 *
 * Usage: https://jooby.io/modules/openapi
 *
 * @author edgar
 * @since 2.7.0
 */
public class OpenAPITask extends BaseTask {

  private String mainClassName;

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

    String mainClass = Optional.ofNullable(mainClassName)
        .orElseGet(() -> computeMainClassName(projects));

    ClassLoader classLoader = createClassLoader(projects);

    getLogger().info(" Generating OpenAPI: " + mainClass);

    getLogger().debug("Using classloader: " + classLoader);

    String[] names = mainClass.split("\\.");
    Path dir = Stream.of(names)
        .reduce(classes(getProject()), Path::resolve, Path::resolve)
        .getParent();

    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setClassLoader(classLoader);
    tool.setOutputDir(dir);
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
  public @Nonnull String getMainClassName() {
    return mainClassName;
  }

  /**
   * Set Class to parse.
   * @param mainClassName Class to parse.
   */
  public void setMainClassName(@Nonnull String mainClassName) {
    this.mainClassName = mainClassName;
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

  private Optional<String> trim(String value) {
    if (value == null || value.trim().length() == 0) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }
}
