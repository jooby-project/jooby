/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.flyway;

import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FlywayModule implements Extension {

  private final String name;

  public FlywayModule(@Nonnull String name) {
    this.name = name;
  }

  public FlywayModule() {
    this("db");
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Environment environment = application.getEnvironment();
    ServiceRegistry registry = application.getServices();
    DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    if (dataSource == null) {
      // TODO: replace with usage exception
      dataSource = registry.require(DataSource.class);
    }
    FluentConfiguration configuration = new FluentConfiguration(environment.getClassLoader());

    Map<String, String> defaults = environment.getProperties("flyway");
    Map<String, String> overrides = environment.getProperties(name + ".flyway", "flyway");

    Map<String, String> properties = new HashMap<>();
    properties.putAll(defaults);
    properties.putAll(overrides);

    String[] commandString = Optional.ofNullable(properties.remove("run")).orElse("migrate")
        .split("\\s*,\\s*");

    configuration.configuration(properties);
    configuration.dataSource(dataSource);

    Flyway flyway = new Flyway(configuration);

    for (String command : commandString) {
      runCommand(command.toLowerCase(), flyway);
    }

    registry.putIfAbsent(Flyway.class, flyway);
    registry.put(ServiceKey.key(Flyway.class, name), flyway);
  }

  private void runCommand(String command, Flyway flyway) {
    switch (command) {
      case "migrate":
        flyway.migrate();
        break;
      case "clean":
        flyway.clean();
        break;
      case "info":
        flyway.info();
        break;
      case "validate":
        flyway.validate();
        break;
      case "undo":
        flyway.undo();
        break;
      case "baseline":
        flyway.baseline();
      case "repair":
        flyway.repair();
      default:
        throw new IllegalArgumentException("Unknown flyway command: " + command);
    }
  }
}
