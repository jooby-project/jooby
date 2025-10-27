/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.logback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import io.jooby.LoggingService;
import io.jooby.SneakyThrows;

/** Logback logging system. */
public class LogbackService implements LoggingService {
  @Override
  public String getPropertyName() {
    return "logback.configurationFile";
  }

  @Override
  public List<String> getLogFileName() {
    return List.of("logback.xml");
  }

  @Override
  public void logOff(List<String> logger, SneakyThrows.Runnable task) {
    Map<String, Level> state = new HashMap<>();
    try {
      for (String it : logger) {
        state.put(it, setLogbackLevel(it, Level.OFF));
      }
      task.run();
    } finally {
      for (Map.Entry<String, Level> e : state.entrySet()) {
        setLogbackLevel(e.getKey(), e.getValue());
      }
    }
  }

  private static Level setLogbackLevel(String name, Level level) {
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
    var existingLevel = logger.getLevel();
    logger.setLevel(level);
    return existingLevel;
  }
}
