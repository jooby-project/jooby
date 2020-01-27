/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.ebean;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueType;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.Properties;

/**
 * Persistence module using Ebean: https://ebean.io.
 *
 * <pre>{@code
 * {
 *    install(new HikariModule());
 *
 *    install(new EbeanModule());
 *
 *    get("/", ctx -> {
 *
 *      Database db = require(Database.class);
 *      ...
 *    });
 * }
 * }</pre>
 *
 * We do recommend the use of Hikari as connection pool, so make sure the hikari module is in your
 * project dependencies.
 *
 * Ebean depends on an annotation processor to do byte code enhancements. Please check
 * https://ebean.io/docs/getting-started/maven for maven setup and
 * https://ebean.io/docs/getting-started/gradle for gradle setup.
 *
 * The module integrates Ebean with Jooby application properties and service registry (require
 * calls or DI framework).
 *
 * <h2>Properties</h2>
 * <p>
 *   Module checks for `ebean.[name]` and `ebean.*` properties (in that order) and creates a
 *   {@link DatabaseConfig}. Check {@link #create(Jooby, String)}.
 * </p>
 *
 * @since 2.6.1
 * @author edgar
 */
public class EbeanModule implements Extension {
  private final String name;
  private final DatabaseConfig databaseConfig;

  /**
   * Creates a new ebean module.
   *
   * @param name Ebean name.
   */
  public EbeanModule(@Nonnull String name) {
    this.name = name;
    this.databaseConfig = null;
  }

  /**
   * Creates a new ebean module using the default name: <code>db</code>.
   */
  public EbeanModule() {
    this("db");
  }

  /**
   * Creates a module using the provided database configuration.
   *
   * @param config Database configuration.
   */
  public EbeanModule(@Nonnull DatabaseConfig config) {
    this.databaseConfig = config;
    this.name = databaseConfig.getName();
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    DatabaseConfig config = Optional.ofNullable(this.databaseConfig)
        .orElseGet(() -> create(application, name));

    Database database = DatabaseFactory.create(config);
    ServiceRegistry services = application.getServices();
    services.putIfAbsent(Database.class, database);
    services.put(ServiceKey.key(Database.class, name), database);
  }

  /**
   * Creates a new/default database configuration object from application configuration properties.
   * This method look for ebean properties at:
   *
   * - ebean.[name]
   * - ebean
   *
   * At look at ebean[.name] and fallbacks to [ebean].
   *
   * @param application Application.
   * @param name Ebean name.
   * @return Database configuration.
   */
  public static @Nonnull DatabaseConfig create(@Nonnull Jooby application, @Nonnull String name) {
    Environment environment = application.getEnvironment();
    ServiceRegistry registry = application.getServices();
    DatabaseConfig databaseConfig = new DatabaseConfig();
    Properties properties = new Properties();

    Config config = environment.getConfig();
    Config ebean = config(config, "ebean." + name)
        .withFallback(config(config, "ebean"));

    ebean.entrySet().forEach(e -> properties.put(e.getKey(), e.getValue().unwrapped().toString()));
    databaseConfig.loadFromProperties(properties);

    if (!ebean.hasPath("datasource")) {
      DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
      if (dataSource == null) {
        dataSource = registry.get(DataSource.class);
      }
      databaseConfig.setDataSource(dataSource);
    }
    return databaseConfig;
  }

  private static Config config(Config config, String name) {
    if (config.hasPath(name) && config.getValue(name).valueType() == ConfigValueType.OBJECT) {
      return config.getConfig(name);
    }
    return ConfigFactory.empty();
  }
}
