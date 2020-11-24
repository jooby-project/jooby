/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
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
import java.util.Optional;
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

  private Config config;

  private final ClassLoader classLoader;

  /**
   * Creates a new environment.
   *
   * @param classLoader Class loader.
   * @param config Application configuration.
   * @param actives Active environment names.
   */
  public Environment(@Nonnull ClassLoader classLoader, @Nonnull Config config,
      @Nonnull String... actives) {
    this(classLoader, config, Arrays.asList(actives));
  }

  /**
   * Creates a new environment.
   *
   * @param classLoader Class loader.
   * @param config Application configuration.
   * @param actives Active environment names.
   */
  public Environment(@Nonnull ClassLoader classLoader, @Nonnull Config config,
      @Nonnull List<String> actives) {
    this.classLoader = classLoader;
    this.actives = actives.stream()
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
    this.config = config;
  }

  /**
   * Get a property under the given key or use the given default value when missing.
   *
   * @param key Property key.
   * @param defaults Default value.
   * @return Property or default value.
   */
  public @Nonnull String getProperty(@Nonnull String key, @Nonnull String defaults) {
    if (hasPath(config, key)) {
      return config.getString(key);
    }
    return defaults;
  }

  /**
   * Get a property under the given key or <code>null</code> when missing.
   *
   * @param key Property key.
   * @return Property value or <code>null</code> when missing.
   */
  public @Nullable String getProperty(@Nonnull String key) {
    if (hasPath(config, key)) {
      return config.getString(key);
    }
    return null;
  }

  /**
   * List all the properties under the given key. Example:
   *
   * <pre>
   * user.name = "name"
   * user.password = "pass"
   * </pre>
   *
   * A call to <code>getProperties("user")</code> give you a map like:
   * <code>{user.name: name, user.password: pass}</code>
   *
   * @param key Key.
   * @return Properties under that key or empty map.
   */
  public @Nonnull Map<String, String> getProperties(@Nonnull String key) {
    return getProperties(key, key);
  }

  /**
   * List all the properties under the given key. Example:
   *
   * <pre>
   * user.name = "name"
   * user.password = "pass"
   * </pre>
   *
   * A call to <code>getProperties("user", "u")</code> give you a map like:
   * <code>{u.name: name, u.password: pass}</code>
   *
   * @param key Key.
   * @param prefix Prefix to use or <code>null</code> for none.
   * @return Properties under that key or empty map.
   */
  public @Nonnull Map<String, String> getProperties(@Nonnull String key, @Nullable String prefix) {
    if (hasPath(config, key)) {
      Map<String, String> settings = new HashMap<>();
      String p = prefix == null || prefix.length() == 0 ? "" : prefix + ".";
      config.getConfig(key).entrySet().stream()
          .forEach(e -> {
            Object value = e.getValue().unwrapped();
            if (value instanceof List) {
              value = ((List) value).stream().collect(Collectors.joining(", "));
            }
            String k = p + e.getKey();
            settings.put(k, value.toString());
          });
      return settings;
    }
    return Collections.emptyMap();
  }

  /**
   * Application configuration.
   *
   * @return Application configuration.
   */
  public @Nonnull Config getConfig() {
    return config;
  }

  /**
   * Set configuration properties. Please note setting a configuration object must be done at very
   * early application stage.
   *
   * @param config Configuration properties.
   * @return This environment.
   */
  public Environment setConfig(@Nonnull Config config) {
    this.config = config;
    return this;
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
  public boolean isActive(@Nonnull String name, String... names) {
    return this.actives.contains(name.toLowerCase())
        || Stream.of(names).map(String::toLowerCase).anyMatch(this.actives::contains);
  }

  /**
   * Application class loader.
   *
   * @return Application class loader.
   */
  public @Nonnull ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Loaded class or empty.
   *
   * @param className Class name.
   * @return Load a class or get an empty value.
   */
  public @Nonnull Optional<Class> loadClass(@Nonnull String className) {
    try {
      return Optional.of(classLoader.loadClass(className));
    } catch (ClassNotFoundException x) {
      return Optional.empty();
    }
  }

  @Override public String toString() {
    return actives + "\n" + toString(config).trim();
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

  private static boolean hasPath(Config config, String key) {
    try {
      return config.hasPath(key);
    } catch (ConfigException x) {
      return false;
    }
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
    Path userdir = Paths.get(System.getProperty("user.dir"));
    /** Application file: */

    String[] names = new String[actives.size() + 1];
    for (int i = 0; i < actives.size(); i++) {
      names[i] = filename + "." + actives.get(i).trim().toLowerCase() + extension;
    }
    names[actives.size()] = filename + extension;

    Config application = resolveConfig(options, userdir, names);

    // check if there is a local env set
    if (application.hasPath("application.env")) {
      String env = application.getString("application.env");
      // Override environment only if the active environment is set to `dev`
      if (!actives.contains(env) && (actives.contains("dev") && actives.size() == 1)) {
        Config envConfig = resolveConfig(options, userdir,
            filename + "." + env.toLowerCase() + extension);
        if (envConfig != null) {
          application = envConfig.withFallback(application);
          actives = Collections.singletonList(env.toLowerCase());
        }
      }
    }

    Config result = sys
        .withFallback(application)
        .withFallback(defaults())
        .resolve();

    return new Environment(options.getClassLoader(), result, actives);
  }

  private static Config resolveConfig(@Nonnull EnvironmentOptions options, Path userdir,
      String... names) {
    Config application = ConfigFactory.empty();

    String basedir = options.getBasedir();

    Path[] rootdirs;
    String[] cpdirs;
    if (basedir == null) {
      rootdirs = new Path[]{userdir.resolve("conf"), userdir};
      cpdirs = new String[]{"conf", ""};
    } else {
      rootdirs = new Path[]{Paths.get(basedir)};
      cpdirs = new String[]{basedir};
    }

    for (String name : names) {
      Config it = fileConfig(userdir, rootdirs, name);
      if (it == null) {
        // classpath
        it = classpathConfig(options.getClassLoader(), cpdirs, name);
      }
      if (it != null) {
        application = application.withFallback(it);
      }
    }
    return application;
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

  private static Config fileConfig(Path userdir, Path[] basedirs, String name) {
    for (Path basedir : basedirs) {
      Path file = basedir.resolve(name);
      if (Files.exists(file)) {
        String origin = file.startsWith(userdir)
            ? userdir.relativize(file).toString()
            : file.toString();
        return ConfigFactory.parseFile(file.toFile(),
            ConfigParseOptions.defaults().setOriginDescription(origin));
      }
    }
    return null;
  }

  private static Config classpathConfig(ClassLoader classLoader, String[] basedirs, String name) {
    for (String basedir : basedirs) {
      String file = basedir.isEmpty()
          ? name
          : Stream.concat(Stream.of(basedir.split("/")), Stream.of(name))
          .collect(Collectors.joining("/"));
      Config config = ConfigFactory.parseResources(classLoader, file,
          ConfigParseOptions.defaults().setOriginDescription("classpath://" + file));
      if (!config.isEmpty()) {
        return config;
      }
    }
    return null;
  }
}
