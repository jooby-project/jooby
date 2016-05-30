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
package org.jooby.rx;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.jdbc.Jdbc;

import com.github.davidmoten.rx.jdbc.Database;
import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>rxjdbc</h1>
 * <p>
 * <a href="https://github.com/davidmoten/rxjava-jdbc">rxjava-jdbc</a> efficient execution, concise
 * code, and functional composition of database calls using JDBC and RxJava Observable.
 * </p>
 *
 * <p>
 * This module depends on {@link Jdbc} and {@link Rx} modules, make sure you read the doc of the {@link Jdbc}
 * and {@link Rx} modules before using {@link RxJdbc}.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>A {@link Database} object</li>
 * <li>A Hikari {@link DataSource} object</li>
 * </ul>
 *
 * <h2>depends on</h2>
 * <ul>
 * <li>{@link Rx rx module}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 *
 * import org.jooby.rx.RxJdbc;
 * import org.jooby.rx.Rx;
 *
 * {
 *   // required by RxJdbc
 *   use(new Rx());
 *
 *   use(new RxJdbc());
 *
 *   get("/reactive", req ->
 *     req.require(Database.class)
 *       .select("select name from something where id = :id")
 *       .parameter("id", 1)
 *       .getAs(String.class)
 *   );
 * }
 * }</pre>
 *
 * <h2>multiple db connections</h2>
 *
 * <pre>{@code
 *
 * import org.jooby.rx.RxJdbc;
 * import org.jooby.rx.Rx;
 *
 * {
 *   // required by RxJdbc
 *   use(new Rx());
 *
 *   use(new RxJdbc("db.main"));
 *   use(new RxJdbc("db.audit"));
 *
 *   get("/", req ->
 *     Databse db = req.require("db.main", Database.class);
 *     Databse audit = req.require("db.audit", Database.class);
 *     // ...
 *   ).map(Rx.rx());
 * }
 * }</pre>
 *
 * <p>
 * For more details on how to configure the Hikari datasource, please check the {@link Jdbc jdbc
 * module}
 * </p>
 *
 * <p>
 * Happy coding!!!
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public class RxJdbc extends Jdbc {

  /**
   * Creates a new {@link RxJdbc} module.
   *
   * @param name Database name.
   */
  public RxJdbc(final String name) {
    super(name);
  }

  /**
   * Creates a new {@link RxJdbc} module.
   */
  public RxJdbc() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    super.configure(env, config, binder);

    Provider<Database> p = () -> {
      DataSource ds = dataSource().get();
      return Database.fromDataSource(ds);
    };

    keys(Database.class, k -> {
      binder.bind(k).toProvider(p).asEagerSingleton();

      // close on shutdown
      env.onStop(r -> r.require(k).close());
    });
  }
}
