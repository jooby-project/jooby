/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rainbowgum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import io.jooby.Environment;
import io.jooby.LoggingService;
import io.jooby.SneakyThrows;
import io.jstach.rainbowgum.LevelResolver;
import io.jstach.rainbowgum.LogConfig;
import io.jstach.rainbowgum.LogProperties;
import io.jstach.rainbowgum.RainbowGum;
import io.jstach.rainbowgum.ServiceRegistry;
import io.jstach.rainbowgum.spi.RainbowGumServiceProvider;

/** Configures Rainbow Gum from Jooby's resolved application configuration. */
public final class RainbowGumService implements LoggingService {
  private static final Object LOCK = new Object();
  private static final Map<String, Integer> MUTED = new HashMap<>();
  private static final LogProperties.MutableLogProperties LEVELS =
      LogProperties.MutableLogProperties.builder()
          .with(new ConcurrentHashMap<>())
          .description("Jooby temporary logger levels")
          .build();

  static final LogProperties DEFAULTS =
      LogProperties.builder()
          .fromProperties("logging.change.mode=on\nlogging.change.logger=level")
          .description("Jooby logging defaults")
          .order(-1000)
          .build();

  enum Owner {
    INSTANCE
  }

  /** For service loading. */
  public RainbowGumService() {}

  static void register(ServiceRegistry registry) {
    registry.put(Owner.class, Owner.INSTANCE);
    registry.put(LogProperties.class, LevelResolver.OVERRIDE_PROPERTIES_NAME, LEVELS);
  }

  @Override
  public void initialize(Environment environment) {
    synchronized (LOCK) {
      var previous = RainbowGum.getOrNull();
      if (previous != null && previous.config().serviceRegistry().findOrNull(Owner.class) == null) {
        // An explicitly installed Rainbow Gum remains under the application's control.
        return;
      }
      var registry = ServiceRegistry.of();
      register(registry);
      var properties =
          LogProperties.of(
              List.of(
                  LogProperties.StandardProperties.SYSTEM_PROPERTIES,
                  new ConfigProperties(environment::getConfig),
                  DEFAULTS));
      var config =
          LogConfig.builder()
              .properties(properties)
              .serviceRegistry(registry)
              .serviceLoader(
                  ServiceLoader.load(RainbowGumServiceProvider.class, environment.getClassLoader()))
              .build();
      // Validate and construct before replacing the working global instance.
      var gum = RainbowGum.builder(config).build();
      RainbowGum.set(gum).get();
      if (previous != null) {
        previous.close();
      }
    }
  }

  @Override
  public String getPropertyName() {
    return "logging.config";
  }

  @Override
  public List<String> getLogFileName() {
    // Logging is part of the application environment, not a separately discovered file.
    return List.of();
  }

  @Override
  public void logOff(List<String> loggers, SneakyThrows.Runnable task) {
    var names = loggers.stream().map(RainbowGumService::levelKey).distinct().toList();
    synchronized (LOCK) {
      for (var name : names) {
        MUTED.merge(name, 1, Integer::sum);
        LEVELS.put(name, "off");
      }
    }
    try {
      synchronized (LOCK) {
        publish();
      }
      task.run();
    } finally {
      synchronized (LOCK) {
        for (var name : names) {
          int remaining = MUTED.get(name) - 1;
          if (remaining == 0) {
            MUTED.remove(name);
            LEVELS.put(name, null);
          } else {
            MUTED.put(name, remaining);
          }
        }
        publish();
      }
    }
  }

  private static String levelKey(String name) {
    return name.isEmpty() || name.equals("ROOT")
        ? LogProperties.LEVEL_PREFIX
        : LogProperties.LEVEL_PREFIX + "." + name;
  }

  private static void publish() {
    var gum = RainbowGum.getOrNull();
    if (gum != null) {
      gum.config().changePublisher().publish();
    }
  }
}
