/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import io.jooby.run.HotSwap;
import org.gradle.StartParameter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JoobyRun extends DefaultTask {

  static {
    System.setProperty("jooby.useShutdownHook", "false");
  }

  private String executionMode = "DEFAULT";

  private List<String> restartExtensions = Arrays.asList("conf", "properties", "class");

  private List<String> compileExtensions = Arrays.asList("java", "kt");

  private ProjectConnection connection;

  @TaskAction
  public void run() throws Throwable {
    try {
      Project current = getProject();
      List<Project> projects = Arrays.asList(current);

      String mainClass = projects.stream()
          .filter(it -> it.getProperties().containsKey("mainClassName"))
          .map(it -> it.getProperties().get("mainClassName").toString())
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
              "Application class not found. Did you forget to set `mainClassName`?"));

      HotSwap hotSwap = new HotSwap(current.getName(), mainClass, executionMode);

      connection = GradleConnector.newConnector()
          .useInstallation(current.getGradle().getGradleHomeDir())
          .forProjectDirectory(current.getRootDir())
          .connect();

      BuildLauncher compiler = connection.newBuild()
          .setStandardError(System.err)
          .setStandardOutput(System.out)
          .forTasks("classes");

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        hotSwap.shutdown();
        connection.close();
      }));

      BiConsumer<String, Path> onFileChanged = (event, path) -> {
        if (isCompileExtension(path)) {
          compiler.run(new ResultHandler<Void>() {
            @Override public void onComplete(Void result) {
              getLogger().debug("Restarting application on file change: " + path);
              hotSwap.restart();
            }

            @Override public void onFailure(GradleConnectionException failure) {
              getLogger().debug("Compilation error found: " + path);
            }
          });
        } else if (isRestartExtension(path)) {
          getLogger().debug("Restarting application on file change: " + path);
          hotSwap.restart();
        } else {
          getLogger().debug("Ignoring file change: " + path);
        }
      };

      for (Project project : projects) {
        getLogger().debug("Adding project: " + project.getName());

        SourceSet sourceSet = sourceSet(project);
        // main/resources
        sourceSet.getResources().getSrcDirs().stream()
            .map(File::toPath)
            .forEach(file -> hotSwap.addResource(file, onFileChanged));
        // conf directory
        Path conf = project.getProjectDir().toPath().resolve("conf");
        hotSwap.addResource(conf, onFileChanged);

        // build classes
        binDirectories(project, sourceSet).forEach(hotSwap::addResource);

        Set<Path> src = sourceDirectories(project, sourceSet);
        if (src.isEmpty()) {
          getLogger().debug("Compiler is off in favor of Eclipse compiler.");
          binDirectories(project, sourceSet)
              .forEach(path -> hotSwap.addResource(path, onFileChanged));
        } else {
          src.forEach(path -> hotSwap.addResource(path, onFileChanged));
        }

        dependencies(project, sourceSet).forEach(hotSwap::addResource);
      }

      // Block current thread.
      hotSwap.start();
    } catch (InvocationTargetException x) {
      throw x.getCause();
    }
  }

  private Set<Path> binDirectories(Project project, SourceSet sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && Files.isDirectory(it));
  }

  private Set<Path> dependencies(Project project, SourceSet sourceSet) {
    return classpath(project, sourceSet, it -> Files.exists(it) && it.toString().endsWith(".jar"));
  }

  private Set<Path> classpath(Project project, SourceSet sourceSet, Predicate<Path> predicate) {
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

  private Set<Path> sourceDirectories(Project project, SourceSet sourceSet) {
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

  private SourceSet sourceSet(final Project project) {
    return getJavaConvention(project).getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
  }

  public JavaPluginConvention getJavaConvention(final Project project) {
    return project.getConvention().getPlugin(JavaPluginConvention.class);
  }

  private boolean isCompileExtension(Path path) {
    return containsExtension(compileExtensions, path);
  }

  private boolean isRestartExtension(Path path) {
    return containsExtension(restartExtensions, path);
  }

  private boolean containsExtension(List<String> extensions, Path path) {
    String filename = path.getFileName().toString();
    return extensions.stream().anyMatch(ext -> filename.endsWith("." + ext));
  }

  public String getExecutionMode() {
    return executionMode;
  }

  public void setExecutionMode(String executionMode) {
    this.executionMode = executionMode;
  }

  public List<String> getRestartExtensions() {
    return restartExtensions;
  }

  public void setRestartExtensions(List<String> restartExtensions) {
    this.restartExtensions = restartExtensions;
  }

  public List<String> getCompileExtensions() {
    return compileExtensions;
  }

  public void setCompileExtensions(List<String> compileExtensions) {
    this.compileExtensions = compileExtensions;
  }
}
