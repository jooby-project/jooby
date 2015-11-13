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
package org.jooby.jdbc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;

/**
 * Production-ready jdbc data source, powered by the
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> library.
 *
 * <h1>Usage</h1>
 *
 * <pre>
 * import org.jooby.jdbc.Jdbc;
 * import javax.sql.DataSource;
 *
 * {
 *   use(new Jdbc());
 *
 *   // accessing to the data source
 *   get("/my-api", (req, rsp) {@literal ->} {
 *     DataSource db = req.getInstance(DataSource.class);
 *     // do something with datasource
 *   });
 * }
 * </pre>
 *
 * <h1>db configuration</h1> Database configuration is controlled from your
 * <code>application.conf</code> file using the <code>db</code> property and friends:
 * <code>db.*</code>.
 *
 * <h2>mem db</h2>
 *
 * <pre>
 *   db = mem
 * </pre>
 *
 * Mem db is implemented with <a href="http://www.h2database.com/">h2 database</a>, before using it
 * make sure to add the h2 dependency to your <code>pom.xml</code>:
 *
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.h2database&lt;/groupId&gt;
 *     &lt;artifactId&gt;h2&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 * </pre>
 *
 * Mem db is useful for dev environment and/or transient data that can be regenerated.
 *
 * <h2>fs db</h2>
 *
 * <pre>
 *   db = fs
 * </pre>
 *
 * File system db is implemented with <a href="http://www.h2database.com/">h2 database</a>, before
 * using it make sure to add the h2 dependency to your ```pom.xml```:
 *
 * File system db is useful for dev environment and/or transient data that can be
 * regenerated. Keep in mind this db is saved in a tmp directory and db will be deleted it
 * on restarts.
 *
 * <h2>db.url</h2> Connect to a database using a jdbc url, some examples here:
 *
 * <pre>
 *   # mysql
 *   db.url = jdbc:mysql://localhost/mydb
 *   db.user=myuser
 *   db.password=password
 * </pre>
 *
 * Previous example, show you how to connect to <strong>mysql</strong>, setting user and password.
 * But of course you need the jdbc driver on your <code>pom.xml</code>:
 *
 * <h2>hikari configuration</h2>
 * <p>
 * If you need to configure or tweak the <a
 * href="https://github.com/brettwooldridge/HikariCP">hikari pool</a> just add <code>hikari.*</code>
 * entries to your <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 *   db.url = jdbc:mysql://localhost/mydb
 *   db.user=myuser
 *   db.password=password
 *   db.cachePrepStmts=true
 *
 *   # hikari
 *   hikari.autoCommit = true
 *   hikari.maximumPoolSize = 20
 *   # etc...
 * </pre>
 *
 * <p>
 * Also, all the <code>db.*</code> properties are converted to <code>dataSource.*</code> to let <a
 * href="https://github.com/brettwooldridge/HikariCP">hikari</a> configure the target jdbc
 * connection.
 * </p>
 *
 * <h2>multiple connections</h2>
 * It is pretty simple to configure two or more db connections.
 *
 * Let's suppose we have a main database and an audit database for tracking changes:
 *
 * <pre>
 * {
 *   use(new Jdbc("db.main")); // main database
 *   use(new Jdbc("db.audit")); // audit database
 * }
 * </pre>
 *
 * <p>application.conf</p>
 *
 * <pre>
 * # main database
 * db.main.url = ...
 * db.main.user=...
 * db.main.password = ...
 *
 * # audit
 * db.audit.url = ....
 * db.audit.user = ....
 * db.audit.password = ....
 * </pre>
 *
 * <p>Same principle applies if you need to tweak hikari:</p>
 *
 * <pre>
 * # max pool size for main db
 * hikari.db.main.maximumPoolSize = 100
 *
 * # max pool size for audit db
 * hikari.db.audit.maximumPoolSize = 20
 * </pre>
 *
 * <p>
 * Finally, if you need to inject the audit data source, all you have to do is to use the
 * <strong>Name</strong> annotation, like <code>@Name("db.audit")</code>
 * </p>
 *
 *
 * That's all folks! Enjoy it!!!
 *
 * @author edgar
 * @since 0.1.0
 */
public class Jdbc implements Jooby.Module {

  private static final String DEFAULT_DB = "db";

  protected final String dbName;

  private HikariDataSourceProvider ds;

  private Optional<String> dbtype;

  public Jdbc(final String name) {
    checkArgument(name != null && name.length() > 0, "A database name is required.");
    this.dbName = name;
  }

  public Jdbc() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {
    this.ds = newDataSource(dbName, dbConfig(dbName, config));

    keys(DataSource.class, key -> binder.bind(key).toProvider(ds).asEagerSingleton());
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Jdbc.class, "jdbc.conf");
  }

  private Config dbConfig(final String key, final Config source) {
    Object db = source.getAnyRef(key);

    if (db instanceof String) {
      String embeddeddb = "databases." + db;
      if (db.toString().indexOf(':') == -1 && source.hasPath(embeddeddb)) {
        Config dbtree = source.getConfig(embeddeddb);
        dbtree = dbtree.withValue("url", ConfigValueFactory.fromAnyRef(
            dbtree.getString("url").replace("{mem.seed}", System.currentTimeMillis() + "")
            ));
        // write embedded with current key
        return ConfigFactory.empty()
            .withValue(key, dbtree.root())
            .withFallback(source);
      } else {
        // assume it is a just the url
        return ConfigFactory.empty()
            .withValue(key + ".url", ConfigValueFactory.fromAnyRef(db.toString()))
            .withFallback(source);
      }
    } else {
      return source;
    }
  }

  private HikariDataSourceProvider newDataSource(final String key, final Config config) {
    Properties props = new Properties();

    BiConsumer<String, Entry<String, ConfigValue>> dumper = (prefix, entry) -> {
      String propertyName = prefix + entry.getKey();
      String[] path = propertyName.split("\\.");

      if (path.length <= 2) {
        String propertyValue = entry.getValue().unwrapped().toString();
        props.setProperty(propertyName, propertyValue);
      }
    };

    String hikaryKey = "hikari" + (DEFAULT_DB.equals(key) ? "" : key.replace(DEFAULT_DB, ""));
    Config $hikari = config.hasPath(hikaryKey) ? config.getConfig(hikaryKey) : ConfigFactory
        .empty();

    // figure it out db type.
    dbtype = dbtype(key, config);

    /**
     * dump properties from less to higher precedence
     *
     * # databases.[type]
     * # db.* -> dataSource.*
     * # hikari.* -> * (no prefix)
     */
    dbtype.ifPresent(type ->
        config.getConfig("databases." + type)
            .entrySet().forEach(entry -> dumper.accept("dataSource.", entry))
        );

    config.getConfig(key).entrySet().forEach(entry -> dumper.accept("dataSource.", entry));

    $hikari.entrySet().forEach(entry -> dumper.accept("", entry));

    if (!props.containsKey("dataSourceClassName")) {
      // adjust dataSourceClassName when missing
      props.setProperty("dataSourceClassName", props.getProperty("dataSource.dataSourceClassName"));
    }
    // remove dataSourceClassName under dataSource
    props.remove("dataSource.dataSourceClassName");
    // set pool name
    props.setProperty("poolName", dbtype.map(type -> type + "." + dbName).orElse(dbName));

    return new HikariDataSourceProvider(new HikariConfig(props));
  }

  private Optional<String> dbtype(final String key, final Config config) {
    String url = config.getString(key + ".url");
    String type = Arrays.stream(url.toLowerCase().split(":"))
        .filter(token -> !(token.equals("jdbc") || token.equals("jtds")))
        .findFirst()
        .get();

    if (config.hasPath("databases." + type)) {
      return Optional.of(type);
    }
    return Optional.empty();
  }

  /**
   * Build keys for the given resource type. When database name is: <code>db</code> two keys
   * (binding) are generated, once without a name and other with a name. A database: <code>db</code>
   * is considered the default database.
   *
   * @param type A type to bind.
   * @param callback A generated key.
   * @param <T> Consumer type.
   */
  protected final <T> void keys(final Class<T> type, final Consumer<Key<T>> callback) {
    if (DEFAULT_DB.equals(dbName)) {
      callback.accept(Key.get(type));
    }
    callback.accept(Key.get(type, Names.named(dbName)));
  }

  protected final Provider<DataSource> dataSource() {
    checkState(ds != null, "Data source isn't ready yet");
    return ds;
  }
}
