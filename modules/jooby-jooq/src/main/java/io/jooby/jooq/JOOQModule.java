/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jooq;

import com.typesafe.config.Config;
import io.jooby.*;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;
import org.jooq.tools.jdbc.JDBCUtils;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * jOOQ module:  https://jooby.io/modules/jooq.
 *
 * Usage:
 *
 * - Add hikari and jOOQ dependency
 *
 * - Install them
 *
 * <pre>{@code
 * {
 *   install(new HikariModule());
 *
 *   install(new JOOQModule());
 * }
 * }</pre>
 *
 * - Use it
 *
 * <pre>{@code
 * {
 *   get("/", ctx -> {
 *     try (DSLContext dsl = require(DSLContext.class)) {
 *       // do with jooq
 *     }
 *   });
 * }
 * }</pre>
 *
 * usage with database property key (see HikariModule)
 *
 *  * <pre>{@code
 *  * {
 *  *   install(new HikariModule("jooby_makes_fun_db"));
 *  *
 *  *   install(new JOOQModule("jooby_makes_fun_db"));
 *  * }
 *  * }</pre>
 *  *
 *  * - Use it
 *  *
 *  * <pre>{@code
 *  * {
 *  *   get("/", ctx -> {
 *  *     try (DSLContext dsl = require(DSLContext.class, "jooby_makes_fun_db")) {
 *  *       // do with jooq
 *  *     }
 *  *   });
 *  * }
 *  * }</pre>
 *
 */
public class JOOQModule implements Extension {

  private final String name;

  private BiConsumer<Configuration, Config> callback;

  /**
   * Creates a new {@link JOOQModule} module.
   *
   * @param database name of database.
   */
  public JOOQModule(final String database) {
    this.name = database;
  }

  /**
   * Creates a new {@link JOOQModule} module with default database <i>db</i>
   */
  public JOOQModule() {
    this("db");
  }

  /**
   * Configuration callback.
   *
   * @param configurer Callback.
   * @return This module.
   */
  public JOOQModule doWith(BiConsumer<Configuration, Config> configurer) {
    this.callback = configurer;
    return this;
  }

  /**
   * Configuration callback.
   *
   * @param configurer Callback.
   * @return This module.
   */
  public JOOQModule doWith(Consumer<Configuration> configurer) {
    return doWith((configuration, conf) -> configurer.accept(configuration));
  }

  @Override
  public void install(@Nonnull Jooby application) throws Exception {
    Environment env = application.getEnvironment();
    Config config = application.getConfig();
    ServiceRegistry registry = application.getServices();

    DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    if (dataSource == null) {
      // TODO moh-sushi 2021-11-19: replace with usage exception
      dataSource = registry.require(DataSource.class);
    }
    Configuration jooqconf = new DefaultConfiguration();
    ConnectionProvider dscp = new DataSourceConnectionProvider(dataSource);
    jooqconf.set(JDBCUtils.dialect(
        Objects.requireNonNull(env.getProperty(name + ".url"), () -> "no db-url found under " + name + ".url"))
    );
    jooqconf.set(dscp);
    jooqconf.set(new DefaultTransactionProvider(dscp));

    if (callback != null) {
      callback.accept(jooqconf, config);
    }

    registry.putIfAbsent(Configuration.class, jooqconf);
    registry.put(ServiceKey.key(Configuration.class, name), jooqconf);
    registry.putIfAbsent(DSLContext.class, DSL.using(jooqconf));
    registry.put(ServiceKey.key(DSLContext.class, name), DSL.using(jooqconf));
  }
}
