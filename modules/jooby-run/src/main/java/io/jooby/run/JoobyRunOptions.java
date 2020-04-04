/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Jooby run options. Group available option for jooby:run which are exposes via Maven and Gradle
 * plugins.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyRunOptions {

  private String projectName;

  private String mainClass;

  private List<String> restartExtensions = Arrays.asList("conf", "properties", "class");

  private List<String> compileExtensions = Arrays.asList("java", "kt");

  private Integer port = null;

  /**
   * Project name.
   *
   * @return Project name.
   */
  public String getProjectName() {
    return projectName;
  }

  /**
   * Set project name.
   *
   * @param projectName Project name.
   */
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  /**
   * Main class to run.
   *
   * @return Main class (one with main method).
   */
  public String getMainClass() {
    return mainClass;
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
   * Application port.
   *
   * @return Application port.
   */
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
    if (restartExtensions != null && !restartExtensions.isEmpty()) {
      this.restartExtensions = restartExtensions;
    }
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
    if (compileExtensions != null && !compileExtensions.isEmpty()) {
      this.compileExtensions = compileExtensions;
    }
  }

  /**
   * Test if the given path matches a compile extension.
   *
   * @param path File.
   * @return Test if the given path matches a compile extension.
   */
  public boolean isCompileExtension(Path path) {
    return containsExtension(compileExtensions, path);
  }

  /**
   * Test if the given path matches a restart extension.
   *
   * @param path File.
   * @return Test if the given path matches a restart extension.
   */
  public boolean isRestartExtension(Path path) {
    return containsExtension(restartExtensions, path);
  }

  private boolean containsExtension(List<String> extensions, Path path) {
    String filename = path.getFileName().toString();
    return extensions.stream().anyMatch(ext -> filename.endsWith("." + ext));
  }

  @Override public String toString() {
    return "{"
        + "projectName='" + projectName + '\''
        + ", mainClass='" + mainClass + '\''
        + ", restartExtensions=" + restartExtensions
        + ", compileExtensions=" + compileExtensions
        + ", port=" + port
        + '}';
  }
}
