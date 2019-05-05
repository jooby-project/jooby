/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Application environment contains configuration object and active environment names.
 *
 * The active environment names serve the purpose of allowing loading different configuration files
 * depending on the environment. Also, {@link Extension} modules might configure application
 * services differently depending on the environment too. For example: turn on/off caches,
 * reload files, etc.
 *
 * The <code>application.env</code> property controls the active environment names.
 *
 * @since 2.0.0
 * @author edgar
 */
public class Environment {

  private final List<String> actives;

  private final Config conf;

  private final ClassLoader classLoader;

  /**
   * Creates a new environment.
   *
   * @param classLoader Class loader.
   * @param conf Application configuration.
   * @param actives Active environment names.
   */
  public Environment(@Nonnull ClassLoader classLoader, @Nonnull Config conf, @Nonnull String... actives) {
    this(classLoader, conf, Arrays.asList(actives));
  }

  /**
   * Creates a new environment.
   *
   * @param classLoader Class loader.
   * @param conf Application configuration.
   * @param actives Active environment names.
   */
  public Environment(@Nonnull ClassLoader classLoader, @Nonnull Config conf, @Nonnull List<String> actives) {
    this.classLoader = classLoader;
    this.actives = actives.stream()
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
    this.conf = conf;
  }

  /**
   * Application configuration.
   *
   * @return Application configuration.
   */
  public @Nonnull Config getConfig() {
    return conf;
  }

  /**
   * Active environment names.
   *
   * @return Active environment names.
   */
  public @Nonnull List<String> getActiveNames() {
    return Collections.unmodifiableList(actives);
  }

  /**
   * Test is the given environment names are active.
   *
   * @param name Environment name.
   * @param names Optional environment names.
   * @return True if any of the given names is active.
   */
  public boolean isActive(String name, String... names) {
    return this.actives.contains(name.toLowerCase())
        || Stream.of(names).map(String::toLowerCase).anyMatch(this.actives::contains);
  }

  /**
   * Application class loader.
   *
   * @return Application class loader.
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override public String toString() {
    return actives + "\n" + toString(conf).trim();
  }

  private String toString(final Config conf) {
    return configTree(conf.origin().description());
  }

  private String configTree(final String description) {
    return configTree(description.split(":\\s+\\d+,|,"), 0);
  }

  private String configTree(final String[] sources, final int i) {
    char[] pad = new char[i];
    Arrays.fill(pad, ' ');
    if (i < sources.length) {
      return new StringBuilder()
          .append(pad)
          .append("└── ")
          .append(sources[i].replace("merge of", "").trim())
          .append("\n")
          .append(configTree(sources, i + 1))
          .toString();
    }
    return "";
  }

  /**
   * Creates a {@link Config} object from {@link System#getProperties()}.
   *
   * @return Configuration object.
   */
  public static @Nonnull Config systemProperties() {
    return ConfigFactory.parseProperties(System.getProperties(),
        ConfigParseOptions.defaults().setOriginDescription("system properties"));
  }

  /**
   * Creates a {@link Config} object from {@link System#getenv()}.
   *
   * @return Configuration object.
   */
  public static @Nonnull Config systemEnv() {
    return ConfigFactory.systemEnvironment();
  }

  /**
   * This method search for an application.conf file in three location
   * (first-listed are higher priority):
   *
   * <ul>
   *   <li>${user.dir}/conf: This is a file system location, useful is you want to externalize
   *     configuration (outside of jar file).</li>
   *   <li>${user.dir}: This is a file system location, useful is you want to externalize
   *     configuration (outside of jar file)</li>
   *   <li>classpath:// (root of classpath). No external configuration, configuration file lives
   *     inside the jar file</li>
   * </ul>
   *
   * Property overrides is done in the following order (first-listed are higher priority):
   *
   * <ul>
   *   <li>Program arguments</li>
   *   <li>System properties</li>
   *   <li>Environment variables</li>
   *   <li>Environment property file</li>
   *   <li>Property file</li>
   * </ul>
   *
   * @param options Options like basedir, filename, etc.
   * @return A new environment.
   */
  public static @Nonnull Environment loadEnvironment(@Nonnull EnvironmentOptions options) {
    Config sys = systemProperties()
        .withFallback(systemEnv());

    List<String> actives = options.getActiveNames();
    String filename = options.getFilename();
    String extension;
    int ext = filename.lastIndexOf('.');
    if (ext <= 0) {
      extension = ".conf";
    } else {
      extension = filename.substring(ext);
      filename = filename.substring(0, ext);
    }
    String basedir = options.getBasedir();
    Path userdir = Paths.get(System.getProperty("user.dir"));
    /** Application file: */
    Config application = ConfigFactory.empty();
    String[] names = new String[actives.size() + 1];
    for (int i = 0; i < actives.size(); i++) {
      names[i] = filename + "." + actives.get(i).trim().toLowerCase() + extension;
    }
    names[actives.size()] = filename + extension;
    Path fsroot = Paths.get(basedir).toAbsolutePath();
    String[] cproot = basedir.split("/");
    for (String name : names) {
      Path fsfile = fsroot.resolve(name);
      Config it;
      if (Files.exists(fsfile)) {
        String origin = fsfile.startsWith(userdir)
            ? userdir.relativize(fsfile).toString()
            : fsfile.toString();
        it = ConfigFactory.parseFile(fsfile.toFile(),
            ConfigParseOptions.defaults().setOriginDescription(origin));
      } else {
        String cpfile = Stream.concat(Stream.of(cproot), Stream.of(name))
            .collect(Collectors.joining("/"));
        it = ConfigFactory.parseResources(options.getClassLoader(), cpfile,
            ConfigParseOptions.defaults().setOriginDescription("classpath://" + cpfile));
      }
      application = application.withFallback(it);
    }

    Config result = sys
        .withFallback(application)
        .withFallback(defaults())
        .resolve();

    return new Environment(options.getClassLoader(), result, actives);
  }

  /**
   * Creates a default configuration properties with some common values like: application.tmpdir,
   * application.charset and pid (process ID).
   *
   * @return A configuration object.
   */
  public static @Nonnull Config defaults() {
    Path tmpdir = Paths.get(System.getProperty("user.dir"), "tmp");
    Map<String, String> defaultMap = new HashMap<>();
    defaultMap.put("application.tmpdir", tmpdir.toString());
    defaultMap.put("application.charset", "UTF-8");
    String pid = pid();
    if (pid != null) {
      System.setProperty("PID", pid);
      defaultMap.put("pid", pid);
    }

    return ConfigFactory.parseMap(defaultMap, "defaults");
  }

  /**
   * Find JVM process ID.
   * @return JVM process ID or <code>null</code>.
   */
  public static @Nullable String pid() {
    String pid = System.getenv().getOrDefault("PID", System.getProperty("PID"));
    if (pid == null) {
      pid = ManagementFactory.getRuntimeMXBean().getName();
      int i = pid.indexOf("@");
      if (i > 0) {
        pid = pid.substring(0, i);
      }
    }
    return pid;
  }
}
