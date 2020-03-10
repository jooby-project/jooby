/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import io.jooby.openapi.OpenAPIGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

@Mojo(name = "openapi", threadSafe = true,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
    aggregator = true,
    defaultPhase = PROCESS_CLASSES
)
public class OpenAPIMojo extends BaseMojo {

  enum Format {
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

  @Parameter(defaultValue = "json,yaml")
  private String format;

  @Override protected void doExecute(List<MavenProject> projects, String mainClass)
      throws Exception {
    ClassLoader classLoader = createClassLoader(projects);

    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setClassLoader(classLoader);

    getLog().info(" Generating OpenAPI: " + mainClass);

    getLog().debug("Using classloader: " + classLoader);

    OpenAPI result = tool.generate(mainClass);

    String[] names = mainClass.split("\\.");
    Path dir = Stream.of(names)
        .reduce(Paths.get(project.getBuild().getOutputDirectory()), Path::resolve, Path::resolve)
        .getParent();
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
    }

    String name = "openapi";

    for (Format format : Format.parse(this.format)) {
      Path output = dir.resolve(name + "." + format.extension());
      getLog().info("  writing: " + output);

      String content = format.toString(tool, result);
      Files.write(output, Collections.singleton(content));
    }
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

}
