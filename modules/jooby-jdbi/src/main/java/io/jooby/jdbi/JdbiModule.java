/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jdbi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.sql.DataSource;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.jdbi.HandleProvider;
import io.jooby.internal.jdbi.SqlObjectProvider;
import jakarta.inject.Provider;

/**
 * Jdbi module: https://jooby.io/modules/hikari.
 *
 * <p>Usage:
 *
 * <p>- Add hikari and jdbi dependency
 *
 * <p>- Install them
 *
 * <pre>{@code
 * {
 *   install(new HikariModule());
 *
 *   install(new JdbiModule());
 * }
 * }</pre>
 *
 * - Use it
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> {
 *     Jdbi jdbi = require(Jdbi.class);
 *     // do with jdbi
 *   });
 *
 * }
 * }</pre>
 *
 * Handle instances are also available:
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> {
 *     try(Handle handle = require(Handle.class)) {
 *       // do with handle
 *     }
 *   });
 *
 * }
 * }</pre>
 *
 * The use of try-with-resources is required here. Handle must be closed once you finish.
 *
 * <p>For automatic handle managment see the {@link TransactionalRequest} class.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JdbiModule implements Extension {

  private String name;

  private Function<DataSource, Jdbi> factory;

  private List<Class> sqlObjects = Collections.emptyList();

  /**
   * Creates a new Jdbi module using the <code>db</code> property key. This key must be present in
   * the application configuration file, like:
   *
   * <pre>{@code
   * db.url = "jdbc:url"
   * db.user = dbuser
   * db.password = dbpass
   * }</pre>
   */
  public JdbiModule() {
    this("db");
  }

  /**
   * Creates a new Jdbi module.
   *
   * @param name The name/key of the data source to attach.
   */
  public JdbiModule(@NonNull String name) {
    this.name = name;
    this.factory = null;
  }

  /**
   * Creates a new Jdbi module. Use the default/first datasource and register objects using the
   * <code>db</code> key.
   *
   * @param factory Jdbi factory.
   */
  public JdbiModule(@NonNull Function<DataSource, Jdbi> factory) {
    this("db", factory);
  }

  /**
   * Creates a new Jdbi module using the given jdbi factory.
   *
   * @param name Name for registering the service.
   * @param factory Jdbi factory.
   */
  public JdbiModule(@NonNull String name, @NonNull Function<DataSource, Jdbi> factory) {
    this(name);
    this.factory = factory;
  }

  /**
   * Attach SQL objects to a jdbi handle.
   *
   * <p>This method simplify the injection or require of SQL objects. So, it is just a shortcut for
   * {@link Handle#attach(Class)}. Due the SQL objects depends on a Handle this method is only
   * available when the {@link TransactionalRequest} decorator is present.
   *
   * <pre>{@code
   * install(new JdbiModule()
   *   .sqlObjects(UserDAO.class)
   * );
   *
   * use(new TransactionalRequest());
   *
   * get("/users", ctx -> {
   *   UserDAO dao = require(UserDAO.class);
   * });
   * }</pre>
   *
   * @param sqlObjects List of SQL object to register as services.
   * @return This module.
   */
  public @NonNull JdbiModule sqlObjects(@NonNull Class... sqlObjects) {
    this.sqlObjects = Arrays.asList(sqlObjects);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    ServiceRegistry registry = application.getServices();
    Jdbi jdbi;
    if (factory != null) {
      jdbi = factory.apply(findDataSource(registry));
    } else {
      jdbi = Jdbi.create(findDataSource(registry));
      jdbi.installPlugins();
    }
    registry.putIfAbsent(ServiceKey.key(Jdbi.class), jdbi);
    registry.put(ServiceKey.key(Jdbi.class, name), jdbi);

    Provider<Handle> provider = new HandleProvider(jdbi);

    registry.putIfAbsent(ServiceKey.key(Handle.class), provider);
    registry.put(ServiceKey.key(Handle.class, name), provider);

    /** SQLObjects: */
    for (Class<?> sqlObject : sqlObjects) {
      registry.put(sqlObject, new SqlObjectProvider(jdbi, sqlObject));
    }
  }

  private DataSource findDataSource(@NonNull ServiceRegistry registry) {
    DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    if (dataSource == null) {
      // TODO: replace with usage exception
      dataSource = registry.require(DataSource.class);
    }
    return dataSource;
  }
}
