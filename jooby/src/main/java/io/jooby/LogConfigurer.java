/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Utility class that initializes logback or log4j2 logging implementation.
 *
 * <p>Initializes the <code>logback.configurationFile</code> system property when a <code>
 * logback[.env].xml</code> file is found at <code>user.dir/conf</code> directory or <code>user.dir
 * </code>.
 *
 * <p>Initializes the <code>log4j.configurationFile</code> system property when a <code>
 * log4j[.env].[ext]</code> file is found at <code>user.dir/conf</code> directory or <code>user.dir
 * </code>. Extension can be one of: <code>.xml</code>, <code>.properties</code>, <code>.yaml</code>
 * or <code>.json</code>.
 *
 * <p>NOTE: This class must be call it before instantiating a logger instance. Otherwise, this setup
 * is completely ignored.
 *
 * @author edgar
 * @since 2.0.0
 */
public final class LogConfigurer {
  private LogConfigurer() {}

  /**
   * Initializes the <code>logback.configurationFile</code> system property when a <code>
   * logback[.env].xml</code> file is found at <code>user.dir/conf</code> directory or <code>
   * user.dir</code>.
   *
   * <p>Initializes the <code>log4j.configurationFile</code> system property when a <code>
   * log4j[.env].[ext]</code> file is found at <code>user.dir/conf</code> directory or <code>
   * user.dir</code>. Extension can be one of: <code>.xml</code>, <code>.properties</code>, <code>
   * .yaml</code> or <code>.json</code>.
   *
   * @param names Actives environment names. Useful for choosing an environment specific logging
   *     configuration file.
   */
  public static String configure(@NonNull ClassLoader classLoader, @NonNull List<String> names) {
    String[] keys = {"logback.configurationFile", "log4j.configurationFile"};
    for (String key : keys) {
      String file = property(key);
      if (file != null) {
        // found, reset and exit
        System.setProperty(key, file);
        return file;
      }
    }
    Path userdir = Paths.get(System.getProperty("user.dir"));
    Map<String, List<Object>> options = new LinkedHashMap<>();
    options.put("logback.configurationFile", logbackFiles(userdir, names));
    options.put("log4j.configurationFile", log4jFiles(userdir, names));
    // Check file system:
    for (Map.Entry<String, List<Object>> entry : options.entrySet()) {
      Optional<Path> logfile =
          entry.getValue().stream()
              .filter(Path.class::isInstance)
              .map(Path.class::cast)
              .filter(Files::exists)
              .findFirst()
              .map(Path::toAbsolutePath);
      if (logfile.isPresent()) {
        System.setProperty(entry.getKey(), logfile.get().toString());
        return logfile.get().toString();
      }
    }
    // Fallback to classpath:
    for (Map.Entry<String, List<Object>> entry : options.entrySet()) {
      Optional<String> logfile =
          entry.getValue().stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .filter(it -> classLoader.getResource(it) != null)
              .findFirst();
      if (logfile.isPresent()) {
        System.setProperty(entry.getKey(), logfile.get());
        return classLoader.getResource(logfile.get()).toString();
      }
    }
    // nothing found
    return null;
  }

  private static List<Object> logbackFiles(Path basedir, List<String> profiles) {
    return logFile(basedir, profiles, "logback", ".xml");
  }

  private static List<Object> logFile(
      Path basedir, List<String> profiles, String name, String ext) {
    Path confdir = basedir.resolve("conf");
    List<Object> result = new ArrayList<>();
    for (String profile : profiles) {
      String logenvfile = name + "." + profile + ext;
      // conf/logback.[profile].xml | conf/log4j2.[profile].xml
      result.add(confdir.resolve(logenvfile));
      // ./logback.[profile].xml | ./log4j2.[profile].xml
      result.add(basedir.resolve(logenvfile));
      result.add(logenvfile);
    }
    // fallback: logback.xml | log4j2.xml
    String logfile = name + ext;
    // conf/logback.xml | conf/log4j2.xml
    result.add(confdir.resolve(logfile));
    // ./logback.xml | ./log4j2.xml
    result.add(basedir.resolve(logfile));

    // classpath fallback:
    result.add(logfile);

    return result;
  }

  private static List<Object> log4jFiles(Path basedir, List<String> names) {
    List<Object> result = new ArrayList<>();
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
