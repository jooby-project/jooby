/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.jooby.run.JoobyRun;
import io.jooby.run.JoobyRunOptions;

/**
 * Maven plugin for jooby run.
 *
 * @author edgar
 * @since 2.0.0
 */
@Mojo(
    name = "run",
    threadSafe = true,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
    aggregator = true)
@Execute(phase = PROCESS_CLASSES)
public class RunMojo extends BaseMojo {

  static {
    /** Turn off shutdown hook on Server. */
    System.setProperty("jooby.useShutdownHook", "false");
  }

  /**
   * List of file extensions that trigger an application restart. Default is: <code>conf</code>,
   * <code>properties</code> and <code>class</code>.
   */
  @Parameter(property = "jooby.restartExtensions")
  private List<String> restartExtensions;

  /**
   * List of file extensions that trigger a compilation request. Default is: <code>java</code> and
   * <code>kt</code>.
   */
  @Parameter(property = "jooby.compileExtensions")
  private List<String> compileExtensions;

  /** Application port. */
  @Parameter(property = "jooby.port")
  private Integer port;

  /**
   * How long to wait after last file change to restart. Default is: <code>500</code> milliseconds.
   */
  @Parameter(property = "jooby.waitTimeBeforeRestart")
  private Long waitTimeBeforeRestart;

  private boolean useTestScope;

  @Parameter(property = "jooby.useSingleClassLoader")
  private boolean useSingleClassLoader;

  @Override
  protected void doExecute(List<MavenProject> projects, String mainClass) throws Throwable {
    Maven maven = getMaven();
    JoobyRunOptions options = createOptions(mainClass);
    getLog().debug("jooby options: " + options);

    JoobyRun joobyRun = new JoobyRun(options);

    Runtime.getRuntime().addShutdownHook(new Thread(joobyRun::shutdown));

    BiConsumer<String, Path> onFileChanged =
        (event, path) -> {
          if (options.isCompileExtension(path)) {
            MavenExecutionResult result = maven.execute(mavenRequest("process-classes"));
            // Success?
            if (result.hasExceptions()) {
              getLog().debug("Compilation error found: " + path);
            } else {
              getLog().debug("Restarting application on file change: " + path);
              joobyRun.restart(path);
            }
          } else if (options.isRestartExtension(path)) {
            getLog().debug("Restarting application on file change: " + path);
            joobyRun.restart(path);
          } else {
            getLog().debug("Ignoring file change: " + path);
          }
        };

    for (MavenProject project : projects) {
      getLog().debug("Adding project: " + project.getArtifactId());

      // main resources + conf, etc..
      resources(project, useTestScope)
          .forEach(
              file -> {
                joobyRun.addResource(file);
                joobyRun.addWatchDir(file, onFileChanged);
              });

      // target/classes
      bin(project, useTestScope).forEach(joobyRun::addClasses);

      Set<Path> src = sourceDirectories(project, useTestScope);
      if (src.isEmpty()) {
        getLog().debug("Compiler is off in favor of Eclipse compiler.");
        bin(project, useTestScope).forEach(path -> joobyRun.addWatchDir(path, onFileChanged));
      } else {
        src.forEach(path -> joobyRun.addWatchDir(path, onFileChanged));
      }

      jars(project, useTestScope).forEach(joobyRun::addJar);
    }

    // Block current thread.
    joobyRun.start();
  }

  private JoobyRunOptions createOptions(String mainClass) {
    JoobyRunOptions options = new JoobyRunOptions();
    options.setBasedir(project.getBasedir().toPath());
    options.setMainClass(mainClass);
    if (compileExtensions != null) {
      options.setCompileExtensions(compileExtensions);
    }
    options.setPort(port);
    options.setWaitTimeBeforeRestart(waitTimeBeforeRestart);
    options.setProjectName(session.getCurrentProject().getArtifactId());
    if (restartExtensions != null) {
      options.setRestartExtensions(restartExtensions);
    }
    options.setUseSingleClassLoader(useSingleClassLoader);
    return options;
  }

  /**
   * List of file extensions that trigger a compilation request. Compilation is done via Maven or
   * Gradle. Default is: <code>java</code> and <code>kt</code>.
   *
   * @return Compile extensions.
   */
  public List<String> getCompileExtensions() {
    return compileExtensions;
  }

  /**
   * Set compile extensions. Extension is expected to be specify without <code>.</code> (dot).
   *
   * @param compileExtensions Compile extensions.
   */
  public void setCompileExtensions(List<String> compileExtensions) {
    this.compileExtensions = compileExtensions;
  }

  /**
   * List of file extensions that trigger an application restart. Default is: <code>conf</code>,
   * <code>properties</code> and <code>class</code>.
   *
   * @return Restart extensions.
   */
  public List<String> getRestartExtensions() {
    return restartExtensions;
  }

  /**
   * Set restart extensions. Extension is expected to be specify without <code>.</code> (dot).
   *
   * @param restartExtensions Restart extensions.
   */
  public void setRestartExtensions(List<String> restartExtensions) {
    this.restartExtensions = restartExtensions;
  }

  public boolean isUseSingleClassLoader() {
    return useSingleClassLoader;
  }

  public void setUseSingleClassLoader(boolean useSingleClassLoader) {
    this.useSingleClassLoader = useSingleClassLoader;
  }

  protected void setUseTestScope(boolean useTestScope) {
    this.useTestScope = useTestScope;
  }

  /**
   * Execute maven goal.
   *
   * @param goal Goal to execute.
   * @return Request.
   */
  private MavenExecutionRequest mavenRequest(String goal) {
    return DefaultMavenExecutionRequest.copy(session.getRequest())
        .setGoals(Collections.singletonList(goal));
  }

  private Set<Path> sourceDirectories(MavenProject project, boolean useTestScope) {
    Path eclipse = project.getBasedir().toPath().resolve(".classpath");
    if (Files.exists(eclipse)) {
      // let eclipse to do the incremental compilation
      return Collections.emptySet();
    }
    var sourceDir = Paths.get(project.getBuild().getSourceDirectory());
    if (useTestScope) {
      return Set.of(Paths.get(project.getBuild().getTestSourceDirectory()), sourceDir);
    }
    return Collections.singleton(sourceDir);
  }

  @Override
  protected String mojoName() {
    return "run";
  }
}
