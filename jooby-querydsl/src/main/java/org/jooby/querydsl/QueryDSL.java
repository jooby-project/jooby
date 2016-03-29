/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.querydsl;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.typesafe.config.Config;
import org.jooby.Env;
import org.jooby.jdbc.Jdbc;
import org.jooby.querydsl.internal.ConfigurationProvider;
import org.jooby.querydsl.internal.SQLQueryFactoryProvider;
import org.jooby.querydsl.internal.SQLTemplatesProvider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * <h1>QueryDSL</h1>
 * <p>
 * SQL abstraction provided by <a href="http://www.querydsl.com"QueryDSL</a> using plain JDBC underneath.
 * </p>
 *
 * <p>
 * This module depends on {@link Jdbc} module, make sure you read the doc of the {@link Jdbc}
 * module.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * import org.jooby.querydsl.QueryDSL;
 *
 * {
 *   use(new QueryDSL());
 *
 *   get("/my-api", (req, rsp) {@literal ->} {
 *     SQLQueryFactory queryFactory = req.require(SQLQueryFactory.class);
 *     // Do something with the database
 *   });
 * }
 * </pre>
 *
 * <h2>Multiple DBs</h2>
 *
 * <pre>
 * import org.jooby.querydsl.QueryDSL;
 *
 * {
 *   use(new QueryDSL("db.main");
 *   use(new QueryDSL("db.aux");
 *
 *   get("/my-api", (req, rsp) {@literal ->} {
 *     SQLQueryFactory queryFactory = req.require("db.main", SQLQueryFactory.class);
 *     // Do something with the database
 *   });
 * }
 * </pre>
 *
 * <h2>Advanced Configuration</h2>
 * <p>This module builds QueryDSL SQLQueryFactories on top of a default {@link Configuration} object, a
 * {@link SQLTemplates} instance and a {@link javax.sql.DataSource} from the {@link Jdbc} module. Advanced
 * configuration can be added by invoking the {@link #doWith(BiConsumer)} method, adding your own settings to the
 * configuration</p>
 * <pre>
 * {
 *   use(new QueryDSL().doWith(conf {@literal ->} {
 *     conf.set(...);
 *   });
 * }
 * </pre>
 *
 * <h2>Code generation</h2>
 * <p>
 *   This module does not provide code generation for DB mapping classes. To learn how to create QueryDSL mapping
 *   classes using Maven, please consult the
 *   <a href="http://www.querydsl.com/static/querydsl/latest/reference/html_single/#d0e725">QueryDSL documentation.</a>
 * </p>
 *
 *
 * @since 0.17.0-SNAPSHOT
 * @author sjackel
 */
public class QueryDSL extends Jdbc {

  private BiConsumer<Configuration, Config> callback;

  /**
   * Creates a new {@link QueryDSL} module
   * @param name Database name
   */
  public QueryDSL(final String name) {
    super(name);
  }

  /**
   * Creates a new {@link QueryDSL} module
   */
  public QueryDSL() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    super.configure(env, config, binder);

    Provider<SQLTemplates> templatesProvider = new SQLTemplatesProvider(dbtype.orElse("custom"));
    Configuration queryDslConfig = new Configuration(templatesProvider.get());

    if (callback != null) {
      callback.accept(queryDslConfig, config);
    }

    keys(SQLTemplates.class, k -> binder.bind(k).toProvider(templatesProvider));
    keys(Configuration.class, k -> binder.bind(k).toInstance(queryDslConfig));
    keys(SQLQueryFactory.class, k -> binder.bind(k).toProvider(SQLQueryFactoryProvider.class));
  }

  /**
   * Apply advanced configuration
   * @param callback a configuration callback.
   * @return this module.
   */
  public QueryDSL doWith(final Consumer<Configuration> callback) {
    requireNonNull(callback, "Callback needs to be defined");
    return doWith((configuration, conf) -> callback.accept(configuration));
  }

  /**
   * Apply advanced configuration
   * @param callback a configuration callback.
   * @return this module.
   */
  public QueryDSL doWith(final BiConsumer<Configuration, Config> callback) {
    this.callback = requireNonNull(callback, "Callback needs to be defined");
    return this;
  }
}
