/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.flyway;

import java.util.*;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;

/**
 * Flyway database migration module: https://jooby.io/modules/flyway.
 *
 * <p>Usage:
 *
 * <p>- Add hikari and flyway dependency
 *
 * <p>- Install them
 *
 * <pre>{@code
 * {
 *   install(new HikariModule());
 *
 *   install(new FlywayModule());
 * }
 * }</pre>
 *
 * The default command is <code>migrate</code> which is controlled by the <code>flyway.run</code>
 * application configuration property.
 *
 * <p>You can specify multiple commands: <code>flyway.run = [clean, migrate]</code> Complete
 * documentation is available at: https://jooby.io/modules/flyway.
 *
 * @author edgar
 * @since 2.0.0
 */
public class FlywayModule implements Extension {

  private final String name;
  private List<JavaMigration> javaMigrations = List.of();

  /**
   * Creates a new Flyway module.
   *
   * @param name The name/key of the data source to attach.
   */
  public FlywayModule(@NonNull String name) {
    this.name = name;
  }

  /**
   * Creates a new Flyway module. Use the default/first datasource and register objects using the
   * <code>db</code> key.
   */
  public FlywayModule() {
    this("db");
  }

  /**
   * The manually added Java-based migrations. These are not Java-based migrations discovered
   * through classpath scanning and instantiated by Flyway. Instead, these are manually added
   * instances of JavaMigration. This is particularly useful when working with a dependencies, where
   * you may want to instantiate the class and wire up its dependencies.
   *
   * @param migrations The manually added Java-based migrations. An empty array if none.
   * @return This module.
   */
  public FlywayModule javaMigrations(@NonNull JavaMigration... migrations) {
    this.javaMigrations = List.of(migrations);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var environment = application.getEnvironment();
    var registry = application.getServices();
    var dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    if (dataSource == null) {
      dataSource = registry.require(DataSource.class);
    }
    var configuration = new FluentConfiguration(environment.getClassLoader());
    configuration.javaMigrations(javaMigrations.toArray(new JavaMigration[0]));

    var defaults = environment.getProperties("flyway");
    var overrides = environment.getProperties(name + ".flyway", "flyway");

    Map<String, String> properties = new HashMap<>();
    properties.putAll(defaults);
    properties.putAll(overrides);

    var commandString =
        Optional.ofNullable(properties.remove("flyway.run")).orElse("migrate").split("\\s*,\\s*");

    configuration.configuration(properties);
    configuration.dataSource(dataSource);

    var flyway = new Flyway(configuration);

    for (var command : commandString) {
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
        break;
      case "repair":
        flyway.repair();
        break;
      default:
        throw new IllegalArgumentException("Unknown flyway command: " + command);
    }
  }
}
