/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Provides an execution context for application commands as well as utility methods for read and
 * writing to the console.
 *
 * @since 2.0.6
 */
public interface Context {
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
  void writeTemplate(@Nonnull String template, @Nonnull Object model, @Nonnull Path file)
      throws IOException;

  /**
   * Copy a classpath resource to a file.
   *
   * @param source Resource.
   * @param dest Destination file.
   * @throws IOException If something goes wrong.
   */
  void copyResource(@Nonnull String source, @Nonnull Path dest) throws IOException;

  /**
   * Copy a classpath resource to a file.
   *
   * @param source Resource.
   * @param dest Destination file.
   * @param permissions File permissions.
   * @throws IOException If something goes wrong.
   */
  void copyResource(@Nonnull String source, @Nonnull Path dest,
      @Nonnull Set<PosixFilePermission> permissions) throws IOException;

  /**
   * Ask user for input.
   *
   * @param prompt User prompt.
   * @return Input value.
   */
  @Nonnull String readLine(@Nonnull String prompt);

  /**
   * Write a message to console.
   *
   * @param message Message.
   */
  void println(@Nonnull String message);

  /**
   * Jooby version to use.
   *
   * @return Jooby version to use.
   */
  @Nonnull String getVersion();

  /**
   * Working directory (where the projects are created).
   *
   * @return Working directory (where the projects are created).
   */
  @Nonnull Path getWorkspace();

  /**
   * Set workspace/working directory.
   *
   * @param workspace Workspace/working directory.
   */
  void setWorkspace(@Nonnull Path workspace) throws IOException;
}
