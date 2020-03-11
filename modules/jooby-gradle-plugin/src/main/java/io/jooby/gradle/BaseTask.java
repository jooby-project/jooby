/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BaseTask extends DefaultTask {

  protected static final String APP_CLASS = "mainClassName";

  public List<Project> getProjects() {
    return Collections.singletonList(getProject());
  }

  protected String computeMainClassName(List<Project> projects) {
    return projects.stream()
        .filter(it -> it.getProperties().containsKey(APP_CLASS))
        .map(it -> it.getProperties().get(APP_CLASS).toString())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Application class not found. Did you forget to set `" + APP_CLASS + "`?"));
  }

  protected Set<Path> binDirectories(Project project, SourceSet sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && Files.isDirectory(it));
  }

  protected Set<Path> dependencies(Project project, SourceSet sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && it.toString().endsWith(".jar"));
  }

  protected Path classes(Project project) {
     SourceSet sourceSet = sourceSet(project);
    return sourceSet.getRuntimeClasspath().getFiles().stream()
        .filter(f -> f.exists() && f.isDirectory() && f.toString().contains("classes"))
        .findFirst()
        .get()
        .toPath();
  }

  protected Set<Path> classpath(Project project, SourceSet sourceSet, Predicate<Path> predicate) {
    Set<Path> result = new LinkedHashSet<>();
    // classes/main, resources/main + jars
    sourceSet.getRuntimeClasspath().getFiles().stream()
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

  protected Set<Path> sourceDirectories(Project project, SourceSet sourceSet) {
    Path eclipse = project.getProjectDir().toPath().resolve(".classpath");
    if (Files.exists(eclipse)) {
      // let eclipse to do the incremental compilation
      return Collections.emptySet();
    }
    // main/java
    return sourceSet.getAllSource().getSrcDirs().stream()
        .map(File::toPath)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  protected SourceSet sourceSet(final Project project) {
    return getJavaConvention(project).getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
  }

  protected JavaPluginConvention getJavaConvention(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class);
  }

  protected ClassLoader createClassLoader(List<Project> projects)
      throws MalformedURLException {
    List<URL> cp = new ArrayList<>();
    for (Project project : projects) {
      for (Path path : classpath(project, sourceSet(project), it -> true)) {
        cp.add(path.toUri().toURL());
      }

    }
    return toClassLoader(cp, getClass().getClassLoader());
  }

  private static URLClassLoader toClassLoader(final List<URL> cp, final ClassLoader parent) {
    return new URLClassLoader(cp.toArray(new URL[cp.size()]), parent) {
      @Override
      public String toString() {
        return cp.toString();
      }
    };
  }
}
