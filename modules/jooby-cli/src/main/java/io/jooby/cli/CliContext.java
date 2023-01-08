/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides an execution context for application commands as well as utility methods for read and
 * writing to the console.
 *
 * @since 2.0.6
 */
public interface CliContext {
  /**
   * Exit application.
   *
   * @param code Exit code.
   */
  void exit(int code);

  /**
   * Look for the given template and dump/write to the file.
   *
   * @param template Template name.
   * @param model Template data.
   * @param file Output file.
   * @throws IOException If something goes wrong.
   */
  void writeTemplate(@NonNull String template, @NonNull Object model, @NonNull Path file)
      throws IOException;

  /**
   * Copy a classpath resource to a file.
   *
   * @param source Resource.
   * @param dest Destination file.
   * @throws IOException If something goes wrong.
   */
  void copyResource(@NonNull String source, @NonNull Path dest) throws IOException;

  /**
   * Copy a classpath resource to a file.
   *
   * @param source Resource.
   * @param dest Destination file.
   * @param permissions File permissions.
   * @throws IOException If something goes wrong.
   */
  void copyResource(
      @NonNull String source, @NonNull Path dest, @NonNull Set<PosixFilePermission> permissions)
      throws IOException;

  /**
   * List all dependencies and their version. Like:
   *
   * <pre>
   *   kotlin.version = 1.2.3
   * </pre>
   *
   * @return Dependency map.
   * @throws IOException If something goes wrong.
   */
  Map<String, String> getDependencyMap() throws IOException;

  /**
   * Ask user for input.
   *
   * @param prompt User prompt.
   * @return Input value.
   */
  @NonNull String readLine(@NonNull String prompt);

  /**
   * Write a message to console.
   *
   * @param message Message.
   */
  void println(@NonNull String message);

  /**
   * Jooby version to use.
   *
   * @return Jooby version to use.
   */
  @NonNull String getVersion();

  /**
   * Working directory (where the projects are created).
   *
   * @return Working directory (where the projects are created).
   */
  @NonNull Path getWorkspace();

  /**
   * Set workspace/working directory.
   *
   * @param workspace Workspace/working directory.
   * @throws IOException When directory doesn't exist.
   */
  void setWorkspace(@NonNull Path workspace) throws IOException;
}
