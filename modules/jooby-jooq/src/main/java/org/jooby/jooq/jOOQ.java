/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.jooq;

import java.util.function.BiConsumer;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.jdbc.Jdbc;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;
import org.jooq.tools.jdbc.JDBCUtils;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>jOOQ</h1>
 * <p>
 * <a href="http://www.jooq.org">jOOQ</a> generates Java code from your database and lets you build
 * type safe SQL queries through its fluent API.
 * </p>
 *
 * <p>
 * This module depends on {@link Jdbc} module, make sure you read the doc of the {@link Jdbc}
 * module.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>
 * {
 *   use(new jOOQ());
 *
 *   get("/jooq", req {@literal ->} {
 *     try (DSLContext ctx = req.require(DSLContext.class)) {
 *       return ctx.transactionResult(conf {@literal ->} {
 *         DSLContext trx = DSL.using(conf);
 *         return trx.selectFrom(TABLE)
 *             .where(ID.eq(1))
 *             .fetchOne(NAME);
 *       });
 *     }
 *   });
 * }
 * </pre>
 *
 * <h2>multiple db connections</h2>
 *
 * <pre>
 * {
 *   use(new jOOQ("db.main"));
 *   use(new jOOQ("db.audit"));
 *
 *   get("/main", req {@literal ->} {
 *     try (DSLContext ctx = req.require("db.main", DSLContext.class)) {
 *       ...
 *     }
 *   });
 *
 *   get("/audit", req {@literal ->} {
 *     try (DSLContext ctx = req.require("db.audit", DSLContext.class)) {
 *       ...
 *     }
 *   });
 * }
 * </pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * This module setup a {@link Configuration} object with a {@link DataSource} from {@link Jdbc}
 * module and the {@link DefaultTransactionProvider}. More advanced configuration is provided via
 * {@link #doWith(BiConsumer)}:
 * </p>
 * <pre>
 * {
 *   use(new jOOQ().doWith(conf {@literal ->} {
 *     conf.set(...);
 *   });
 * }
 * </pre>
 *
 * <h2>code generation</h2>
 * <p>
 * Unfortunately, this module doesn't provide any built-in facility for code generation. If you need
 * help to setup the code generator please checkout the
 * <a href="http://www.jooq.org/doc/latest/manual/code-generation/">jOOQ documentation</a> for more
 * information.
 * </p>
 *
 * @author edgar
 * @since 0.15.0
 */
public class jOOQ extends Jdbc {

  /**
   * Creates a new {@link jOOQ} module.
   *
   * @param name Database name.
   */
  public jOOQ(final String name) {
    super(name);
  }

  /**
   * Creates a new {@link jOOQ} module.
   */
  public jOOQ() {
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    super.configure(env, conf, binder, (name, ds) -> {
      Configuration jooqconf = new DefaultConfiguration();
      ConnectionProvider dscp = new DataSourceConnectionProvider(ds);
      jooqconf.set(JDBCUtils.dialect(ds.getDataSourceProperties().getProperty("url")));
      jooqconf.set(dscp);
      jooqconf.set(new DefaultTransactionProvider(dscp));

      callback(jooqconf, conf);

      ServiceKey serviceKey = env.serviceKey();
      serviceKey.generate(Configuration.class, name, k -> binder.bind(k).toInstance(jooqconf));

      Provider<DSLContext> dsl = () -> DSL.using(jooqconf);
      serviceKey.generate(DSLContext.class, name, k -> binder.bind(k).toProvider(dsl));
    });
  }
}
