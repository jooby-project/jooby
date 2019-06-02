/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class that initializes logback or log4j2 logging implementation.
 *
 * Initializes the <code>logback.configurationFile</code> system property when a
 * <code>logback[.env].xml</code> file is found at <code>user.dir/conf</code> directory or
 * <code>user.dir</code>.
 *
 * Initializes the <code>log4j.configurationFile</code> system property when a
 * <code>log4j[.env].[ext]</code> file is found at <code>user.dir/conf</code> directory or
 * <code>user.dir</code>. Extension can be one of: <code>.xml</code>, <code>.properties</code>,
 * <code>.yaml</code> or <code>.json</code>.
 *
 * NOTE: This class must be call it before instantiating a logger instance. Otherwise, this setup
 * is completely ignored.
 *
 * @since 2.0.0
 * @author edgar
 */
public final class LogConfigurer {
  private LogConfigurer() {
  }

  /**
   * Initializes the <code>logback.configurationFile</code> system property when a
   * <code>logback[.env].xml</code> file is found at <code>user.dir/conf</code> directory or
   * <code>user.dir</code>.
   *
   * Initializes the <code>log4j.configurationFile</code> system property when a
   * <code>log4j[.env].[ext]</code> file is found at <code>user.dir/conf</code> directory or
   * <code>user.dir</code>. Extension can be one of: <code>.xml</code>, <code>.properties</code>,
   * <code>.yaml</code> or <code>.json</code>.
   *
   * @param names Actives environment names. Useful for choosing an environment specific logging
   *     configuration file.
   */
  public static void configure(@Nonnull List<String> names) {
    String[] keys = {"logback.configurationFile", "log4j.configurationFile"};
    for (String key : keys) {
      String file = property(key);
      if (file != null) {
        // found, reset and exit
        System.setProperty(key, file);
        return;
      }
    }
    Path userdir = Paths.get(System.getProperty("user.dir"));
    Map<String, List<Path>> options = new LinkedHashMap<>();
    options.put("logback.configurationFile", logbackFiles(userdir, names));
    options.put("log4j.configurationFile", log4jFiles(userdir, names));
    for (Map.Entry<String, List<Path>> entry : options.entrySet()) {
      Optional<Path> logfile = entry.getValue().stream().filter(Files::exists)
          .findFirst()
          .map(Path::toAbsolutePath);
      if (logfile.isPresent()) {
        System.setProperty(entry.getKey(), logfile.get().toString());
        break;
      }
    }
  }

  private static List<Path> logbackFiles(Path basedir, List<String> env) {
    return logFile(basedir, env, "logback", ".xml");
  }

  private static List<Path> logFile(Path basedir, List<String> names, String name, String ext) {
    Path confdir = basedir.resolve("conf");
    List<Path> result = new ArrayList<>();
    for (String env : names) {
      String envlogfile = name + "." + env + ext;
      result.add(confdir.resolve(envlogfile));
      result.add(basedir.resolve(envlogfile));
    }
    String logfile = name + ext;
    result.add(confdir.resolve(logfile));
    result.add(basedir.resolve(logfile));
    return result;
  }

  private static List<Path> log4jFiles(Path basedir, List<String> names) {
    List<Path> result = new ArrayList<>();
    String[] extensions = {".properties", ".xml", ".yaml", ".yml", ".json"};
    for (String extension : extensions) {
      result.addAll(logFile(basedir, names, "log4j", extension));
      result.addAll(logFile(basedir, names, "log4j2", extension));
    }
    return result;
  }

  private static String property(String name) {
    return System.getProperty(name, System.getenv(name));
  }
}
