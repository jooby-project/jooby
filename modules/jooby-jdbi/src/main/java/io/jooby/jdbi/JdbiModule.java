/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jdbi;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.jdbi.HandleProvider;
import io.jooby.internal.jdbi.SqlObjectProvider;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class JdbiModule implements Extension {

  private String name;

  private Function<DataSource, Jdbi> factory;

  private List<Class> sqlObjects = Collections.emptyList();

  public JdbiModule() {
    this("db");
  }

  public JdbiModule(@Nonnull String name) {
    this.name = name;
    this.factory = null;
  }

  public JdbiModule(Function<DataSource, Jdbi> factory) {
    this();
    this.factory = factory;
  }

  public JdbiModule(String name, Function<DataSource, Jdbi> factory) {
    this(name);
    this.factory = factory;
  }

  public JdbiModule sqlObjects(Class... sqlObjects) {
    this.sqlObjects = Arrays.asList(sqlObjects);
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
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
      registry.put(sqlObject, new SqlObjectProvider(provider, sqlObject));
    }
  }

  private DataSource findDataSource(@Nonnull ServiceRegistry registry) {
    DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    if (dataSource == null) {
      // TODO: replace with usage exception
      dataSource = registry.require(DataSource.class);
    }
    return dataSource;
  }
}
