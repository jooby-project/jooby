/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Collections.emptyList;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Describe the underlying logging system. Jooby provides two implementation: jooby-logback and
 * jooby-log4j. You might want to add one of these to your classpath, yet still these are optional.
 *
 * @since 3.0.1
 */
public interface LoggingService {
  /**
   * System property to instruct the logging system where the configuration file is. Example: <code>
   * logback.configurationFile = ...</code>
   *
   * @return System property to instruct the logging system where the configuration file is.
   */
  String getPropertyName();

  /**
   * List of possible names for configuration file. Like: <code>logback.xml, log4j.properties, etc..
   * </code>.
   *
   * @return List of possible names for configuration file.
   */
  List<String> getLogFileName();

  /**
   * Utility method to temporarily turn OFF a logger while running an action.
   *
   * @param logger List of logger names.
   * @param task Action to run.
   */
  void logOff(List<String> logger, SneakyThrows.Runnable task);

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
   * @param classLoader Class loader to use.
   * @param names Actives environment names. Useful for choosing an environment specific logging
   *     configuration file.
   * @return Location of logging configuration file or <code>null</code>.
   */
  static @Nullable String configure(@NonNull ClassLoader classLoader, @NonNull List<String> names) {
    // Supported well-know implementation
    String[] keys = {"logback.configurationFile", "log4j.configurationFile"};
    for (String key : keys) {
      String file = property(key);
      if (file != null) {
        // Explicitly set, reset and exit
        System.setProperty(key, file);
        return file;
      }
    }
    var lookup = ServiceLoader.load(LoggingService.class, classLoader).findFirst();
    if (lookup.isEmpty()) {
      // We could throw an exception here but nope, let user choose any other way of setup logging.
      return null;
    }
    // Helps logging lookup on multi-module project, prefer logback from main module
    Path userdir =
        Stream.of(System.getProperty("jooby.dir"), System.getProperty("user.dir"))
            .filter(Objects::nonNull)
            .map(Paths::get)
            .findFirst()
            .orElseThrow((() -> new IllegalStateException("No base directory found")));

    var loggingService = lookup.get();
    var resources = logFiles(userdir, names, loggingService.getLogFileName());
    if (resources.isEmpty()) {
      // We could throw an exception here but nope, let user choose any other way of setup logging.
      return null;
    }

    // Check file system:
    var logPath =
        resources.stream()
            .filter(Path.class::isInstance)
            .map(Path.class::cast)
            .filter(Files::exists)
            .map(Path::toAbsolutePath)
            // Skip build directories from maven/gradle
            .filter(it -> !isBinary(it))
            .findFirst();
    if (logPath.isPresent()) {
      System.setProperty(loggingService.getPropertyName(), logPath.get().toString());
      return logPath.get().toString();
    }
    // Fallback to classpath:
    var logResource =
        resources.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(
                it -> {
                  var found = classLoader.getResource(it);
                  Map.Entry<String, URL> result = null;
                  if (found != null) {
                    result = Map.entry(it, found);
                  }
                  return result;
                })
            .filter(Objects::nonNull)
            .findFirst();

    if (logResource.isPresent()) {
      System.setProperty(loggingService.getPropertyName(), logResource.get().getKey());
      return logResource.get().getValue().toString();
    }
    // nothing found
    return null;
  }

  /**
   * True when path contains one of: <code>target, build, bin</code> directories.
   *
   * @param path Path to test.
   * @return True when path contains one of: <code>target, build, bin</code> directories.
   */
  static boolean isBinary(Path path) {
    var bin = Set.of("target", "build", "bin");
    return StreamSupport.stream(path.spliterator(), false)
        .anyMatch(it -> bin.contains(it.toString()));
  }

  private static List<Object> logFiles(
      Path basedir, List<String> profiles, List<String> logFileNames) {
    for (String logFileName : logFileNames) {
      var extensionStart = logFileName.lastIndexOf('.');
      var fileName = logFileName.substring(0, extensionStart);
      var fileExt = logFileName.substring(extensionStart);
      var files = logFile(basedir, profiles, fileName, fileExt);
      if (!files.isEmpty()) {
        return files;
      }
    }
    return emptyList();
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

  private static @Nullable String property(String name) {
    return System.getProperty(name, System.getenv(name));
  }
}
