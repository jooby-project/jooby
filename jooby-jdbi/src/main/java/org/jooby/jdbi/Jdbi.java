package org.jooby.jdbi;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.jdbc.Jdbc;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.SLF4JLog;

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
 * <h1>Getting Started</h1>
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
 *
 * <h1>Working with SQL Objects</h1>
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
 * <h1>Configuration</h1>
 * <p>
 * If you need to configure and/or customize a {@link DBI} instance, just do:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jdbi().doWith((dbi, config) -> {
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

  private BiConsumer<DBI, Config> configurer;

  private List<Class<?>> sqlObjects;

  public Jdbi(final Class<?>... sqlObjects) {
    this("db", sqlObjects);
  }

  public Jdbi(final String db, final Class<?>... sqlObjects) {
    super(db);
    this.sqlObjects = Lists.newArrayList(sqlObjects);
  }

  public Jdbi doWith(final BiConsumer<DBI, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer is required.");
    return this;
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    super.configure(env, config, binder);

    DBI dbi = new DBI(() -> dataSource().get().getConnection());
    dbi.setSQLLog(new SLF4JLog());

    keys(DBI.class, key -> binder.bind(key).toInstance(dbi));

    keys(Handle.class, key -> binder.bind(key).toProvider(() -> dbi.open()));

    sqlObjects.forEach(sqlObject -> binder.bind(sqlObject)
        .toProvider((Provider) () -> dbi.open(sqlObject))
        );

    if (configurer != null) {
      configurer.accept(dbi, config);
    }
  }

}
