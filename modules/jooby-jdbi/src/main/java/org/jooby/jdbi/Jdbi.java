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
package org.jooby.jdbi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.jdbc.Jdbc;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.ExpandedStmtRewriter;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IterableArgumentFactory;
import org.skife.jdbi.v2.OptionalArgumentFactory;
import org.skife.jdbi.v2.OptionalContainerFactory;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * Exposes {@link DBI}, {@link Handle} and SQL Objects (a.k.a DAO).
 * This module extends the {@link Jdbc} module so all the services provided by the {@link Jdbc}
 * module are inherited.
 *
 * <p>
 * Before start, make sure you already setup a database connection as described in the {@link Jdbc}
 * module.
 * </p>
 *
 * <h1>usage</h1>
 *
 * <p>
 * It is pretty straightforward:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jdbi());
 *
 *   get("/", req {@literal ->} {
 *     DBI dbi = req.require(DBI.class);
 *     // ... work with dbi
 *   });
 *
 *   get("/handle", req {@literal ->} {
 *     try (Handle handle = req.require(Handle.class)) {
 *       // ... work with dbi handle
 *     }
 *   });
 * }
 * </pre>
 * <p>
 * A call to <code>req.require(Handle.class)</code> is the same as:
 * <code>req.require(DBI.class).open(Handle.class)</code>.
 * </p>
 *
 * <h1>sql objects</h1>
 *
 * <p>
 * It is pretty straightforward (too):
 * </p>
 *
 * <pre>
 *
 * public interface MyRepository extends Closeable {
 *   &#64;SqlUpdate("create table something (id int primary key, name varchar(100))")
 *   void createSomethingTable();
 *
 *   &#64;SqlUpdate("insert into something (id, name) values (:id, :name)")
 *   void insert(&#64;ind("id") int id, @Bind("name") String name);
 *
 *   &#64;SqlQuery("select name from something where id = :id")
 *   String findNameById(&#64;Bind("id") int id);
 * }
 *
 * ...
 * {
 *   use(new Jdbi());
 *
 *   get("/handle", req {@literal ->} {
 *     try (MyRepository h = req.require(MyRepository.class)) {
 *       h.createSomethingTable();
 *
 *       h.insert(1, "Jooby");
 *
 *       String name = h.findNameById(1);
 *
 *       return name;
 *     }
 *   });
 * }
 * </pre>
 *
 * <h1>configuration</h1>
 * <p>
 * If you need to configure and/or customize a {@link DBI} instance, just do:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jdbi().doWith((dbi, config) {@literal ->} {
 *     // set custom option
 *   }));
 * }
 * </pre>
 *
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
public class Jdbi extends Jdbc {

  public static final String ARG_FACTORIES = "__argumentFactories_";

  static class DBI2 extends DBI {

    private List<ArgumentFactory<?>> factories = new ArrayList<ArgumentFactory<?>>();

    public DBI2(final ConnectionFactory connectionFactory) {
      super(connectionFactory);
      define(ARG_FACTORIES, factories);
    }

    @Override
    public void registerArgumentFactory(final ArgumentFactory<?> argumentFactory) {
      factories.add(argumentFactory);
      super.registerArgumentFactory(argumentFactory);
    }

  }

  private List<Class<?>> sqlObjects;

  public Jdbi(final Class<?>... sqlObjects) {
    this("db", sqlObjects);
  }

  public Jdbi(final String db, final Class<?>... sqlObjects) {
    super(db);
    this.sqlObjects = Lists.newArrayList(sqlObjects);
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    configure(env, config, binder, (name, ds) -> {
      DBI dbi = new DBI2(() -> ds.getConnection());
      dbi.setSQLLog(new SLF4JLog());
      dbi.registerArgumentFactory(new OptionalArgumentFactory());
      dbi.registerArgumentFactory(new IterableArgumentFactory());
      dbi.registerContainerFactory(new OptionalContainerFactory());
      dbi.setStatementRewriter(new ExpandedStmtRewriter());

      ServiceKey serviceKey = env.serviceKey();
      serviceKey.generate(DBI.class, name, k -> binder.bind(k).toInstance(dbi));
      serviceKey.generate(Handle.class, name, k -> binder.bind(k).toProvider(() -> dbi.open()));

      sqlObjects.forEach(sqlObject -> binder.bind(sqlObject)
          .toProvider((Provider) () -> dbi.open(sqlObject)));

      callback(dbi, config);
    });
  }

}
