/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.model.ReplacedBy;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import io.jooby.run.JoobyRun;
import io.jooby.run.JoobyRunOptions;

/**
 * Gradle plugin for Jooby run.
 *
 * @author edgar
 * @since 2.0.0
 */
public class RunTask extends BaseTask {

  static {
    System.setProperty("jooby.useShutdownHook", "false");
  }

  private ProjectConnection connection;

  private String mainClass;

  private List<String> restartExtensions;

  private List<String> compileExtensions;

  private Integer port;

  /**
   * How long to wait after last file change to restart. Default is: <code>500</code> milliseconds.
   */
  private Long waitTimeBeforeRestart;

  /**
   * Run task.
   *
   * @throws Throwable If something goes wrong.
   */
  @TaskAction
  public void run() throws Throwable {
    try {
      Project current = getProject();
      String[] tasks = current.getGradle().getTaskGraph().getAllTasks().stream()
          .map(Task::getName)
          .filter(name -> !name.equals("joobyRun"))
          .toArray(String[]::new);

      List<Project> projects = getProjects();

      String mainClass = Optional.ofNullable(this.mainClass)
          .orElseGet(() -> computeMainClassName(projects));

      JoobyRunOptions config = new JoobyRunOptions();
      config.setMainClass(mainClass);
      config.setPort(port);
      if (compileExtensions != null) {
        config.setCompileExtensions(compileExtensions);
      }
      if (restartExtensions != null) {
        config.setRestartExtensions(restartExtensions);
      }
      if (waitTimeBeforeRestart != null) {
        config.setWaitTimeBeforeRestart(waitTimeBeforeRestart);
      }
      config.setProjectName(current.getName());
      getLogger().info("jooby options: {}", config);

      JoobyRun joobyRun = new JoobyRun(config);

      connection = GradleConnector.newConnector()
          .useInstallation(current.getGradle().getGradleHomeDir())
          .forProjectDirectory(current.getRootDir())
          .connect();

      BiConsumer<String, Path> onFileChanged = (event, path) -> {
        if (config.isCompileExtension(path)) {
          BuildLauncher compiler = connection.newBuild()
              .setStandardError(System.err)
              .setStandardOutput(System.out)
              .forTasks(tasks);

          compiler.run(new ResultHandler<Void>() {
            @Override public void onComplete(Void result) {
              getLogger().debug("Restarting application on file change: " + path);
              joobyRun.restart(path);
            }

            @Override public void onFailure(GradleConnectionException failure) {
              getLogger().debug("Compilation error found: " + path);
            }
          });
        } else if (config.isRestartExtension(path)) {
          getLogger().debug("Restarting application on file change: " + path);
          joobyRun.restart(path);
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
            .forEach(file -> joobyRun.addResource(file, onFileChanged));
        // conf directory
        Path conf = project.getProjectDir().toPath().resolve("conf");
        joobyRun.addResource(conf, onFileChanged);

        // build classes
        binDirectories(project, sourceSet).forEach(joobyRun::addResource);

        Set<Path> src = sourceDirectories(project, sourceSet);
        if (src.isEmpty()) {
          getLogger().debug("Compiler is off in favor of Eclipse compiler.");
          binDirectories(project, sourceSet)
              .forEach(path -> joobyRun.addResource(path, onFileChanged));
        } else {
          src.forEach(path -> joobyRun.addResource(path, onFileChanged));
        }

        dependencies(project, sourceSet).forEach(joobyRun::addResource);
      }

      safeShutdown(joobyRun::shutdown);

      // Block current thread.
      joobyRun.start();
    } catch (InvocationTargetException x) {
      throw x.getCause();
    }
  }

  /**
   * Main class to run.
   *
   * @return Main class (one with main method).
   */
  @Deprecated
  @ReplacedBy("mainClass")
  public String getMainClassName() {
    return getMainClass();
  }

  /**
   * Main class to run.
   *
   * @return Main class (one with main method).
   */
  @Input
  @org.gradle.api.tasks.Optional
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set main class name.
   *
   * @param mainClassName Main class name.
   */
  @Deprecated
  public void setMainClassName(String mainClassName) {
    setMainClass(mainClassName);
  }

  /**
   * Set main class name.
   *
   * @param mainClass Main class name.
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  /**
   * List of file extensions that trigger an application restart. Default is: <code>conf</code>,
   * <code>properties</code> and <code>class</code>.
   *
   * @return Restart extensions.
   */
  @Input
  @org.gradle.api.tasks.Optional
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

  /**
   * List of file extensions that trigger a compilation request. Compilation is done via Maven or
   * Gradle. Default is: <code>java</code> and <code>kt</code>.
   *
   * @return Compile extensions.
   */
  @Input
  @org.gradle.api.tasks.Optional
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
   * Application port.
   *
   * @return Application port.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public Integer getPort() {
    return port;
  }

  /**
   * Set application port.
   *
   * @param port Application port.
   */
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
   * How long to wait after last file change to restart. Default is: <code>500</code> milliseconds.
   *
   * @return How long to wait after last file change.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public Long getWaitTimeBeforeRestart() {
    return waitTimeBeforeRestart;
  }

  /**
   * Set How long to wait after last file change to restart.
   * Default is: <code>500</code> milliseconds.
   *
   * @param waitTimeBeforeRestart How long to wait after last file change to restart.
   */
  public void setWaitTimeBeforeRestart(Long waitTimeBeforeRestart) {
    this.waitTimeBeforeRestart = waitTimeBeforeRestart;
  }

  /**
   *
   * Shutdown without killing gradle daemon on ENTER KEY.
   *
   * @param shutdown
   */
  private static void safeShutdown(Runnable shutdown) {
    new Thread(() -> {
      waitForCloseSignal();
      shutdown.run();
    }, "jooby-shutdown").start();
  }

  private static void waitForCloseSignal() {
    try (Scanner scanner = new Scanner(System.in)) {
      // wait for enter to shutdown
      scanner.nextLine();
    } catch (NoSuchElementException | IllegalStateException | UncheckedIOException x) {
      // Ctrl+C All IO is disconnected, we are OK
    }
  }
}
