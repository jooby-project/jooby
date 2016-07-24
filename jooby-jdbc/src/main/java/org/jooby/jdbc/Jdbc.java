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
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Jooby;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Try;

/**
 * <h1>jdbc</h1>
 * <p>
 * Production-ready jdbc data source, powered by the
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> library.
 * </p>
 *
 * <h2>usage</h2>
 *
 * Via connection string:
 * <pre>
 * {
 *   use(new Jdbc("jdbc:mysql://localhost/db"));
 *
 *   // accessing to the data source
 *   get("/my-api", (req, rsp) {@literal ->} {
 *     DataSource db = req.getInstance(DataSource.class);
 *     // do something with datasource
 *   });
 * }
 * </pre>
 *
 * Via <code>db</code> property:
 * <pre>
 * {
 *   use(new Jdbc("db"));
 *
 *   // accessing to the data source
 *   get("/my-api", (req, rsp) {@literal ->} {
 *     DataSource db = req.getInstance(DataSource.class);
 *     // do something with datasource
 *   });
 * }
 * </pre>
 *
 * <h2>db configuration</h2>
 * <p>
 * Database configuration is controlled from your <code>application.conf</code> file using the
 * <code>db</code> property and friends: <code>db.*</code>.
 * </p>
 *
 * <h3>mem db</h3>
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
 * <h3>fs db</h3>
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
 * <h3>db.url</h3>
 * <p>
 * Connect to a database using a jdbc url, some examples here:
 * </p>
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
 * <h3>hikari configuration</h3>
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
 * <p>
 * application.conf:
 * </p>
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
 * <p>
 * Same principle applies if you need to tweak hikari:
 * </p>
 *
 * <pre>
 * # max pool size for main db
 * hikari.main.maximumPoolSize = 100
 *
 * # max pool size for audit db
 * hikari.audit.maximumPoolSize = 20
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

  public static Function<String, String> DB_NAME = url -> {
    BiFunction<String, String, Tuple2<String, Map<String, String>>> indexOf = (str, token) -> {
      int i = str.indexOf(token);
      int len = i >= 0 ? i : str.length();
      Map<String, String> params = Splitter.on(token)
          .trimResults()
          .omitEmptyStrings()
          .withKeyValueSeparator('=')
          .split(str.substring(len));
      return Tuple.of(str.substring(0, len), params);
    };
    // strip ; or ?
    Tuple2<String, Map<String, String>> result = indexOf.apply(url, "?");
    Map<String, String> params = new HashMap<>(result._2);
    result = indexOf.apply(result._1, ";");
    params.putAll(result._2);
    List<String> parts = Splitter.on(CharMatcher.JAVA_LETTER_OR_DIGIT.negate())
        .splitToList(result._1);
    return Optional.ofNullable(params.get("database"))
        .orElse(Optional.ofNullable(params.get("databaseName"))
            .orElse(parts.get(parts.size() - 1)));
  };

  private BiConsumer<HikariConfig, Config> conf;

  private final String dbref;

  protected Optional<String> dbtype;

  /**
   * Creates a new {@link Jdbc} module.
   *
   * @param name A connection string or property with a connection string.
   */
  public Jdbc(final String name) {
    checkArgument(name != null && name.length() > 0,
        "Connection String/Database property required.");
    this.dbref = name;
  }

  /**
   * Creates a new {@link Jdbc} module. The <code>db</code> property must be present in your
   * <code>.conf</code> file.
   */
  public Jdbc() {
    this("db");
  }

  /**
   * Programmatically configure a {@link HikariConfig}.
   *
   * @param conf Configurer callback.
   * @return This module.
   */
  public Jdbc doWithHikari(final BiConsumer<HikariConfig, Config> conf) {
    this.conf = requireNonNull(conf, "Configurer required.");
    return this;
  }

  /**
   * Programmatically configure a {@link HikariConfig}.
   *
   * @param conf Configurer callback.
   * @return This module.
   */
  public Jdbc doWithHikari(final Consumer<HikariConfig> conf) {
    requireNonNull(conf, "Configurer required.");
    return doWithHikari((h, c) -> conf.accept(h));
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    configure(env, config, binder, (name, ds) -> {
    });
  }

  protected void configure(final Env env, final Config config, final Binder binder,
      final BiConsumer<String, HikariDataSource> callback) {
    Config dbconf;
    String url, dbname, dbkey;
    boolean seturl = false;
    if (dbref.startsWith("jdbc:")) {
      dbconf = config;
      url = dbref;
      dbname = DB_NAME.apply(url);
      dbkey = dbname;
      seturl = true;
    } else {
      dbconf = dbConfig(dbref, config);
      url = dbconf.getString(dbref + ".url");
      dbname = DB_NAME.apply(url);
      dbkey = dbref;
    }

    HikariConfig hikariConf = hikariConfig(url, dbkey, dbname, dbconf);

    if (seturl) {
      Properties props = hikariConf.getDataSourceProperties();
      props.setProperty("url", url);
    }

    if (conf != null) {
      conf.accept(hikariConf, config);
    }

    HikariDataSource ds = new HikariDataSource(hikariConf);
    callback.accept(dbname, ds);

    env.serviceKey()
        .generate(DataSource.class, dbname, k -> binder.bind(k).toInstance(ds));

    env.onStop(ds::close);
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
            dbtree.getString("url").replace("{mem.seed}", System.currentTimeMillis() + "")));
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

  private HikariConfig hikariConfig(final String url, final String key, final String db,
      final Config config) {
    Properties props = new Properties();

    BiConsumer<String, Entry<String, ConfigValue>> dumper = (prefix, entry) -> {
      String propertyName = prefix + entry.getKey();
      String[] path = propertyName.split("\\.");

      if (path.length <= 2) {
        String propertyValue = entry.getValue().unwrapped().toString();
        props.setProperty(propertyName, propertyValue);
      }
    };

    Function<String, Config> hikari = path -> Try.of(() -> config.getConfig(path))
        .getOrElse(ConfigFactory.empty());

    String dbkey = key.replace("db.", "");
    Config $hikari = hikari.apply("hikari." + dbkey)
        .withFallback(hikari.apply("hikari." + db))
        .withFallback(hikari.apply("hikari"))
        .withoutPath(dbkey)
        .withoutPath(db);

    // figure it out db type.
    dbtype = dbtype(url, config);

    /**
     * dump properties from less to higher precedence
     *
     * # databases.[type]
     * # db.* -> dataSource.*
     * # hikari.* -> * (no prefix)
     */
    dbtype.ifPresent(type -> config.getConfig("databases." + type)
        .entrySet().forEach(entry -> dumper.accept("dataSource.", entry)));

    Try.of(() -> config.getConfig(key))
        .getOrElse(ConfigFactory.empty())
        .entrySet().forEach(entry -> dumper.accept("dataSource.", entry));

    $hikari.entrySet().forEach(entry -> dumper.accept("", entry));

    if (!props.containsKey("dataSourceClassName")) {
      // adjust dataSourceClassName when missing
      props.setProperty("dataSourceClassName", props.getProperty("dataSource.dataSourceClassName"));
    }
    // remove dataSourceClassName under dataSource
    props.remove("dataSource.dataSourceClassName");
    // set pool name
    props.setProperty("poolName", dbtype.map(type -> type + "." + db).orElse(db));

    return new HikariConfig(props);
  }

  private Optional<String> dbtype(final String url, final Config config) {
    String type = Arrays.stream(url.toLowerCase().split(":"))
        .filter(token -> !(token.equals("jdbc") || token.equals("jtds")))
        .findFirst()
        .get();

    return Optional.of(type);
  }

}
