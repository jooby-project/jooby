/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;

public class MutedServer implements Server {
  private Server delegate;

  private List<String> mute;

  private MutedServer(Server server, List<String> mute) {
    this.delegate = server;
    this.mute = mute;
  }

  /**
   * Muted a server when need it.
   *
   * @param server Server to mute.
   * @return Muted server or same server.
   */
  public static Server mute(Server server, String... logger) {
    if (server instanceof MutedServer) {
      return server;
    }
    List<String> mute =
        Stream.concat(server.getLoggerOff().stream(), Stream.of(logger))
            .collect(Collectors.toList());

    return mute.isEmpty() ? server : new MutedServer(server, mute);
  }

  @NonNull public Server setOptions(@NonNull ServerOptions options) {
    return delegate.setOptions(options);
  }

  @NonNull public String getName() {
    return delegate.getName();
  }

  @NonNull public ServerOptions getOptions() {
    return delegate.getOptions();
  }

  @NonNull public Server start(@NonNull Jooby application) {
    return logOff(
        mute,
        () -> {
          delegate.start(application);
          return delegate;
        });
  }

  @NonNull public List<String> getLoggerOff() {
    return delegate.getLoggerOff();
  }

  @NonNull public Server stop() {
    return logOff(
        mute,
        () -> {
          delegate.stop();
          return delegate;
        });
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  private static <T> T logOff(List<String> logger, SneakyThrows.Supplier<T> task) {
    Map<String, String> state = new HashMap<>();
    try {
      for (String it : logger) {
        state.put(it, setLoggerLevel(it, "OFF"));
      }
      return task.get();
    } finally {
      for (Map.Entry<String, String> e : state.entrySet()) {
        setLoggerLevel(e.getKey(), e.getValue());
      }
    }
  }

  private static String setLoggerLevel(String logger, String level) {
    try {
      if (System.getProperty("logback.configurationFile", "").length() > 0) {
        return setLogbackLevel(logger, level);
      }
      if (System.getProperty("log4j.configurationFile", "").length() > 0) {
        return setLog4jLevel(logger, level);
      }
    } catch (NoClassDefFoundError cause) {
      // ignore it
    }
    return null;
  }

  private static String setLogbackLevel(String name, String level) {
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
    var existingLevel =
        Optional.ofNullable(logger.getLevel())
            .map(ch.qos.logback.classic.Level::toString)
            .orElse(null);
    logger.setLevel(level == null ? null : ch.qos.logback.classic.Level.toLevel(level));
    return existingLevel;
  }

  private static String setLog4jLevel(String name, String level) {
    var context = org.apache.logging.log4j.core.LoggerContext.getContext(false);
    var config = context.getConfiguration();
    var logger = config.getLoggerConfig(name);
    // set level
    org.apache.logging.log4j.core.config.Configurator.setLevel(
        name, level == null ? null : org.apache.logging.log4j.Level.toLevel(level));
    // root logger is returned when no logger is set
    if (logger.getName().equals(name)) {
      return Optional.ofNullable(logger.getLevel())
          .map(org.apache.logging.log4j.Level::toString)
          .orElse(null);
    }
    return null;
  }
}
