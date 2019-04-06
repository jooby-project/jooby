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
package io.jooby;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EnvironmentOptions {
  private static final String ENV = "application.env";
  private String basedir;

  private String filename;

  private ClassLoader classLoader;

  private String[] activeNames;

  public EnvironmentOptions() {
    setBasedir(defaultDir());
    setFilename("application.conf");
  }

  public String[] getActiveNames() {
    return activeNames == null ? defaultEnvironmentNames() : activeNames;
  }

  public @Nonnull EnvironmentOptions setActiveNames(@Nonnull String... activeNames) {
    this.activeNames = activeNames;
    return this;
  }

  private static @Nonnull String[] defaultEnvironmentNames() {
    return System.getProperty(ENV, System.getenv().getOrDefault(ENV, "dev")).split(",");
  }

  public @Nonnull ClassLoader getClassLoader() {
    return classLoader == null ? getClass().getClassLoader() : classLoader;
  }

  public @Nonnull ClassLoader getClassLoader(@Nonnull ClassLoader defaultClassLoader) {
    return classLoader == null ? defaultClassLoader : classLoader;
  }

  public @Nonnull EnvironmentOptions setClassLoader(@Nonnull ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public @Nonnull String getBasedir() {
    return basedir;
  }

  public @Nonnull String getFilename() {
    return filename;
  }

  public @Nonnull EnvironmentOptions setBasedir(@Nonnull String basedir) {
    this.basedir = basedir;
    return this;
  }

  public @Nonnull EnvironmentOptions setBasedir(@Nonnull Path basedir) {
    this.basedir = basedir.toAbsolutePath().toString();
    return this;
  }

  public @Nonnull EnvironmentOptions setFilename(@Nonnull String filename) {
    this.filename = filename;
    return this;
  }

  private static Path defaultDir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    Path confdir = userdir.resolve("conf");
    return Files.exists(confdir) ? confdir : userdir;
  }
}
