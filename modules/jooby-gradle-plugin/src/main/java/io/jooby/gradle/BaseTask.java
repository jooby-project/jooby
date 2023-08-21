/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * Base class which provides common utility method to more specific plugins: like classpath
 * resources.
 *
 * Also, handle maven specific exceptions.
 *
 * @author edgar
 */
public class BaseTask extends DefaultTask {

  protected static final String APP_CLASS_NAME = "mainClassName";

  /**
   * Available projects.
   *
   * @return Available projects.
   */
  @Internal
  public @NonNull List<Project> getProjects() {
    return Collections.singletonList(getProject());
  }

  /**
   * Compute class name from available projects.
   *
   * @param projects Projects.
   * @return Main class.
   */
  protected @NonNull String computeMainClassName(@NonNull List<Project> projects) {
    return projects.stream()
        .map(it -> {
          // Old way:
          String mainClassName = Optional.ofNullable(it.getProperties().get(APP_CLASS_NAME))
              .map(Objects::toString)
              .orElseGet(() ->
                // New way:
                Optional.ofNullable(it.getConvention().findByType(JavaApplication.class))
                    .map(plugin -> plugin.getMainClass().getOrNull())
                    .orElse(null)
              );
          return mainClassName;
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Application class not found. Did you forget to set `" + APP_CLASS_NAME + "`?"));
  }

  /**
   * Project binary directories.
   *
   * @param project Project.
   * @param sourceSet Source set.
   * @return Directories.
   */
  protected @NonNull Set<Path> binDirectories(@NonNull Project project,
      @NonNull List<SourceSet> sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && Files.isDirectory(it));
  }

  /**
   * Project dependencies(jars).
   *
   * @param project Project.
   * @param sourceSet Source set.
   * @return Jar files.
   */
  protected @NonNull Set<Path> jars(@NonNull Project project,
      @NonNull List<SourceSet> sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && it.toString().endsWith(".jar"));
  }

  /**
   * Project classes directory.
   *
   * @param project Project.
   * @return Classes directory.
   */
  protected @NonNull Path classes(@NonNull Project project, boolean useTestScope) {
    List<SourceSet> sourceSet = sourceSet(project, useTestScope);
    return sourceSet.stream()
        .flatMap(it -> it.getRuntimeClasspath().getFiles().stream())
        .filter(f -> f.exists() && f.isDirectory() && f.toString().contains("classes"))
        .findFirst()
        .get()
        .toPath();
  }

  /**
   * Project classpath.
   *
   * @param project Project.
   * @param sourceSet Source set.
   * @param predicate Path filter.
   * @return Classpath.
   */
  protected @NonNull Set<Path> classpath(@NonNull Project project, @NonNull List<SourceSet> sourceSet,
      @NonNull Predicate<Path> predicate) {
    Set<Path> result = new LinkedHashSet<>();
    // classes/main, resources/main + jars
    sourceSet.stream()
        .flatMap(it -> it.getRuntimeClasspath().getFiles().stream())
        .map(File::toPath)
        .filter(predicate)
        .forEach(result::add);

    // provided?
    Optional.ofNullable(project.getConfigurations().findByName("provided"))
        .map(Configuration::getFiles)
        .ifPresent(
            files -> files.stream().map(File::toPath).filter(predicate).forEach(result::add));

    return result;
  }

  /**
   * Project source directories.
   *
   * @param project Project.
   * @param sourceSet Source set.
   * @return Source directories.
   */
  protected @NonNull Set<Path> sourceDirectories(@NonNull Project project,
      @NonNull List<SourceSet> sourceSet) {
    Path eclipse = project.getProjectDir().toPath().resolve(".classpath");
    if (Files.exists(eclipse)) {
      // let eclipse to do the incremental compilation
      return Collections.emptySet();
    }
    // main/java
    return sourceSet.stream()
        .flatMap(it -> it.getAllSource().getSrcDirs().stream())
        .map(File::toPath)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Source set.
   *
   * @param project Project.
   * @return SourceSet.
   */
  protected @NonNull List<SourceSet> sourceSet(@NonNull Project project, boolean useTestScope) {
    SourceSetContainer sourceSets = getJavaConvention(project).getSourceSets();
    List<SourceSet> result = new ArrayList<>();
    if (useTestScope) {
      result.add(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME));
    }
    result.add(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME));
    return result;
  }

  /**
   * Java plugin convention.
   *
   * @param project Project.
   * @return Java plugin convention.
   */
  protected @NonNull JavaPluginExtension getJavaConvention(final @NonNull Project project) {
    return project.getExtensions().getByType(JavaPluginExtension.class);
  }

  /**
   * Creates a class loader.
   *
   * @param projects Projects to  use.
   * @return Class loader.
   * @throws MalformedURLException If there is a bad path reference.
   */
  protected ClassLoader createClassLoader(List<Project> projects)
      throws MalformedURLException {
    List<URL> cp = new ArrayList<>();
    for (Project project : projects) {
      for (Path path : classpath(project, sourceSet(project, false), it -> true)) {
        cp.add(path.toUri().toURL());
      }

    }
    return toClassLoader(cp, getClass().getClassLoader());
  }

  private static URLClassLoader toClassLoader(final List<URL> cp, final ClassLoader parent) {
    return new URLClassLoader(cp.toArray(new URL[0]), parent) {
      @Override
      public String toString() {
        return cp.toString();
      }
    };
  }
}
