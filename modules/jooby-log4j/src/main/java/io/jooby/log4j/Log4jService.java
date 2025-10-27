/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.log4j;

import static org.apache.logging.log4j.core.config.Configurator.setLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import io.jooby.LoggingService;
import io.jooby.SneakyThrows;

/** Configure jooby to use log4j. */
public class Log4jService implements LoggingService {
  /** Default constructor. */
  public Log4jService() {}

  @Override
  public String getPropertyName() {
    return "log4j.configurationFile";
  }

  @Override
  public List<String> getLogFileName() {
    return List.of(
        "log4j.xml",
        "log4j.json",
        "log4j.yaml",
        "log4j.yml",
        "log4j.properties",
        "log4j2.xml",
        "log4j2.json",
        "log4j2.yaml",
        "log4j2.yml",
        "log4j2.properties");
  }

  @Override
  public void logOff(List<String> logger, SneakyThrows.Runnable task) {
    var context = LoggerContext.getContext(false);
    var config = context.getConfiguration();
    Map<String, Level> state = new HashMap<>();
    try {
      for (String it : logger) {
        state.put(it, setLog4jLevel(config, it, Level.OFF));
      }
      task.run();
    } finally {
      for (Map.Entry<String, Level> e : state.entrySet()) {
        setLog4jLevel(config, e.getKey(), e.getValue());
      }
    }
  }

  private static Level setLog4jLevel(Configuration config, String name, Level level) {
    var logger = config.getLoggerConfig(name);
    // set level
    setLevel(name, level);
    // root logger is returned when no logger is set
    if (logger.getName().equals(name)) {
      return logger.getLevel();
    }
    return null;
  }
}
