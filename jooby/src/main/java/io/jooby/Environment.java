/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Application environment contains configuration object and active environment names.
 *
 * <p>The active environment names serve the purpose of allowing loading different configuration
 * files depending on the environment. Also, {@link Extension} modules might configure application
 * services differently depending on the environment too. For example: turn on/off caches, reload
 * files, etc.
 *
 * <p>The <code>application.env</code> property controls the active environment names.
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
  public Environment(
      @NonNull ClassLoader classLoader, @NonNull Config config, @NonNull String... actives) {
    this(classLoader, config, List.of(actives));
  }

  /**
   * Creates a new environment.
   *
   * @param classLoader Class loader.
   * @param config Application configuration.
   * @param actives Active environment names.
   */
  public Environment(
      @NonNull ClassLoader classLoader, @NonNull Config config, @NonNull List<String> actives) {
    this.classLoader = classLoader;
    this.actives =
        actives.stream().map(String::trim).map(String::toLowerCase).collect(Collectors.toList());
    this.config = config;
  }

  /**
   * Get a property under the given key or use the given default value when missing.
   *
   * @param key Property key.
   * @param defaults Default value.
   * @return Property or default value.
   */
  public @NonNull String getProperty(@NonNull String key, @NonNull String defaults) {
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
  public @Nullable String getProperty(@NonNull String key) {
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
   * A call to <code>getProperties("user")</code> give you a map like: <code>
   * {user.name: name, user.password: pass}</code>
   *
   * @param key Key.
   * @return Properties under that key or empty map.
   */
  public @NonNull Map<String, String> getProperties(@NonNull String key) {
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
   * A call to <code>getProperties("user", "u")</code> give you a map like: <code>
   * {u.name: name, u.password: pass}</code>
   *
   * @param key Key.
   * @param prefix Prefix to use or <code>null</code> for none.
   * @return Properties under that key or empty map.
   */
  public @NonNull Map<String, String> getProperties(@NonNull String key, @Nullable String prefix) {
    if (hasPath(config, key)) {
      Map<String, String> settings = new HashMap<>();
      String p = prefix == null || prefix.isEmpty() ? "" : prefix + ".";
      config
          .getConfig(key)
          .entrySet()
          .forEach(
              e -> {
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
  public @NonNull Config getConfig() {
    return config;
  }

  /**
   * Set configuration properties. Please note setting a configuration object must be done at very
   * early application stage.
   *
   * @param config Configuration properties.
   * @return This environment.
   */
  public Environment setConfig(@NonNull Config config) {
    this.config = config;
    return this;
  }

  /**
   * Active environment names.
   *
   * @return Active environment names.
   */
  public @NonNull List<String> getActiveNames() {
    return Collections.unmodifiableList(actives);
  }

  /**
   * Test is the given environment names are active.
   *
   * @param name Environment name.
   * @param names Optional environment names.
   * @return True if any of the given names is active.
   */
  public boolean isActive(@NonNull String name, String... names) {
    return this.actives.contains(name.toLowerCase())
        || Stream.of(names).map(String::toLowerCase).anyMatch(this.actives::contains);
  }

  /**
   * Application class loader.
   *
   * @return Application class loader.
   */
  public @NonNull ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Loaded class or empty.
   *
   * @param className Class name.
   * @return Load a class or get an empty value.
   */
  public @NonNull Optional<Class> loadClass(@NonNull String className) {
    try {
      return Optional.of(classLoader.loadClass(className));
    } catch (ClassNotFoundException x) {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    return actives + "; " + toString(config).trim();
  }

  private String toString(final Config conf) {
    return configTree(conf.origin().description());
  }

  private String configTree(final String description) {
    return Stream.of(description.split(":\\s+\\d+,|,"))
        .map(it -> it.replace("merge of", ""))
        .collect(Collectors.joining(" > "));
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
  public static @NonNull Config systemProperties() {
    return ConfigFactory.parseProperties(
        System.getProperties(),
        ConfigParseOptions.defaults().setOriginDescription("system properties"));
  }

  /**
   * Creates a {@link Config} object from {@link System#getenv()}.
   *
   * @return Configuration object.
   */
  public static @NonNull Config systemEnv() {
    return ConfigFactory.systemEnvironment();
  }

  /**
   * This method search for an application.conf file in three location (first-listed are higher
   * priority):
   *
   * <ul>
   *   <li>${user.dir}/conf: This is a file system location, useful is you want to externalize
   *       configuration (outside of jar file).
   *   <li>${user.dir}: This is a file system location, useful is you want to externalize
   *       configuration (outside of jar file)
   *   <li>classpath:// (root of classpath). No external configuration, configuration file lives
   *       inside the jar file
   * </ul>
   *
   * Property overrides is done in the following order (first-listed are higher priority):
   *
   * <ul>
   *   <li>Program arguments
   *   <li>System properties
   *   <li>Environment variables
   *   <li>Environment property file
   *   <li>Property file
   * </ul>
   *
   * @param options Options like basedir, filename, etc.
   * @return A new environment.
   */
  public static @NonNull Environment loadEnvironment(@NonNull EnvironmentOptions options) {
    Config sys = systemProperties().withFallback(systemEnv());

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
    if (application.hasPath(AvailableSettings.ENV)) {
      String env = application.getString(AvailableSettings.ENV);
      // Override environment only if the active environment is set to `dev`
      if (!actives.contains(env) && (actives.contains("dev") && actives.size() == 1)) {
        Config envConfig =
            resolveConfig(options, userdir, filename + "." + env.toLowerCase() + extension);
        if (envConfig != null) {
          application = envConfig.withFallback(application);
          actives = Collections.singletonList(env.toLowerCase());
        }
      }
    }

    Config result = sys.withFallback(application).withFallback(defaults()).resolve();

    return new Environment(options.getClassLoader(), result, actives);
  }

  private static Config resolveConfig(
      @NonNull EnvironmentOptions options, Path userdir, String... names) {
    Config application = ConfigFactory.empty();

    String basedir = options.getBasedir();

    Path[] rootdirs;
    String[] cpdirs;
    if (basedir == null) {
      rootdirs = new Path[] {userdir.resolve("conf"), userdir};
      cpdirs = new String[] {"conf", ""};
    } else {
      rootdirs = new Path[] {Paths.get(basedir)};
      cpdirs = new String[] {basedir};
    }

    for (String name : names) {
      Config it = fileConfig(rootdirs, name);
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
  public static @NonNull Config defaults() {
    Path tmpdir = Paths.get(System.getProperty("user.dir"), "tmp");
    Map<String, String> defaultMap = new HashMap<>();
    defaultMap.put(AvailableSettings.TMP_DIR, tmpdir.toString());
    defaultMap.put(AvailableSettings.CHARSET, "UTF-8");
    String pid = pid();
    if (pid != null) {
      System.setProperty("PID", pid);
      defaultMap.put(AvailableSettings.PID, pid);
    }

    return ConfigFactory.parseMap(defaultMap, "defaults");
  }

  /**
   * Find JVM process ID.
   *
   * @return JVM process ID or <code>null</code>.
   */
  public static @Nullable String pid() {
    String pid = System.getenv().getOrDefault("PID", System.getProperty("PID"));
    if (pid == null) {
      return Long.valueOf(ProcessHandle.current().pid()).toString();
    }
    return pid;
  }

  private static Config fileConfig(Path[] basedirs, String name) {
    for (Path basedir : basedirs) {
      Path file = basedir.resolve(name);
      if (Files.exists(file)) {
        return ConfigFactory.parseFile(file.toFile());
      }
    }
    return null;
  }

  private static Config classpathConfig(ClassLoader classLoader, String[] basedirs, String name) {
    for (String basedir : basedirs) {
      String file =
          basedir.isEmpty()
              ? name
              : Stream.concat(Stream.of(basedir.split("/")), Stream.of(name))
                  .collect(Collectors.joining("/"));
      Config config = ConfigFactory.parseResources(classLoader, file);
      if (!config.isEmpty()) {
        return config;
      }
    }
    return null;
  }
}
