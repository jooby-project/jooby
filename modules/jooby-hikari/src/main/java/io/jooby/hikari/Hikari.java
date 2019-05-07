/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hikari;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValueType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Hikari implements Extension {

  public static class Builder {

    public HikariConfig build(Environment env) {
      return build(env, "db");
    }

    public HikariConfig build(Environment env, String database) {
      String dburl, dbkey;
      database = builtindb(env, database);
      if (database.startsWith("jdbc:")) {
        dbkey = databaseName(database);
        dburl = database;
      } else {
        Config conf = env.getConfig();
        dburl = Stream.of(database + ".url", database)
            .filter(key -> conf.hasPath(key)
                && conf.getValue(key).valueType() == ConfigValueType.STRING)
            .findFirst()
            .map(conf::getString)
            .orElse(null);
        dbkey = database;
      }

      return build(env, dbkey, builtindb(env, dburl));
    }

    private String builtindb(Environment env, String database) {
      if ("mem".equals(database)) {
        return "jdbc:h2:mem:" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1";
      } else if ("fs".equals(database)) {
        Path path = Paths
            .get(env.getConfig().getString("application.tmpdir"),
                env.getConfig().getString("application.name"));
        return "jdbc:h2:" + path.toAbsolutePath();
      }
      return database;
    }

    private HikariConfig build(Environment env, String dbkey, String dburl) {
      Properties properties = new Properties();
      String dbtype, dbname;
      if (dburl == null) {
        dbtype = null;
        dbname = null;
      } else {
        properties.setProperty("dataSource.url", dburl);
        dbtype = databaseType(dburl);
        dbname = databaseName(dburl);
      }

      /** defaults: */
      properties.putAll(defaults(dbtype, env));
      /** db.* :*/
      props(env, (prop, v) -> {
        if (prop.endsWith(DATASOURCE_CLASS_NAME)) {
          properties.put(DATASOURCE_CLASS_NAME, v);
        } else {
          properties.put("dataSource." + prop, v);
        }
      }, dbkey);

      /** Hikari: */
      props(env, (prop, v) -> {
        properties.put(prop, v);
      }, "hikari", "hikari." + dbkey, "hikari." + dbname);

      /** Rebuild database url, type and name. */
      if (properties.containsKey("driverClassName")) {
        properties.remove(DATASOURCE_CLASS_NAME);
        properties.setProperty("jdbcUrl", dburl);
      }
      String poolname;
      dburl = properties.getProperty("jdbcUrl", properties.getProperty("dataSource.url", dburl));
      if (dburl == null) {
        dbtype = properties
            .getProperty(DATASOURCE_CLASS_NAME,
                properties.getProperty("dataSource.database", "driverClassName"));
        dbtype = dbtype.substring(dbtype.lastIndexOf('.') + 1)
            .replace("DataSource", "")
            .toLowerCase();
        dbname = properties.getProperty("dataSource.database", dbtype);
        poolname = dbtype + "." + dbname;
      } else {
        poolname = dbtype + "." + dbname;
      }
      properties.setProperty("poolName", poolname);

      return new HikariConfig(properties);
    }

    private void props(Environment env, BiConsumer<String, String> consumer, String... keys) {
      for (String key : keys) {
        try {
          Config conf = env.getConfig();
          if (conf.hasPath(key) && conf.getValue(key).valueType() == ConfigValueType.OBJECT) {
            conf.getConfig(key).root().unwrapped().forEach((k, v) -> {
              if (v instanceof String) {
                consumer.accept(k, (String) v);
              }
            });
          }
        } catch (ConfigException.BadPath | ConfigException.BugOrBroken expected) {
          // do nothing
        }
      }
    }
  }

  static {
    System.setProperty("log4jdbc.auto.load.popular.drivers", "false");
  }

  private static final String DATASOURCE_CLASS_NAME = "dataSourceClassName";

  private static final Set<String> SKIP_TOKENS = Stream.of("jdbc", "jtds")
      .collect(Collectors.toSet());

  public static final ServiceKey<DataSource> KEY = ServiceKey.key(DataSource.class);

  private HikariConfig hikari;

  private String database;

  public Hikari(HikariConfig hikari) {
    this.hikari = hikari;
    this.database = hikari.getPoolName();
  }

  public Hikari(@Nonnull String database) {
    this.database = database;
  }

  public Hikari() {
    this("db");
  }

  @Override public void install(@Nonnull Jooby application) {
    if (hikari == null) {
      hikari = create().build(application.getEnvironment(), database);
    }
    HikariDataSource dataSource = new HikariDataSource(hikari);

    ServiceRegistry registry = application.getServices();
    ServiceKey<DataSource> key = ServiceKey.key(DataSource.class, database);
    /** Global default database: */
    registry.putIfAbsent(KEY, dataSource);

    /** Specific access: */
    registry.put(key, dataSource);

    application.onStop(dataSource::close);
  }

  public static Builder create() {
    return new Builder();
  }

  public static String databaseType(String url) {
    String type = Arrays.stream(url.toLowerCase().split(":"))
        .filter(token -> !SKIP_TOKENS.contains(token))
        .findFirst()
        .orElse(url);
    return type;
  }

  public static String databaseName(String url) {
    int len = url.length();
    int q = url.indexOf('?');
    if (q == -1) {
      q = len;
    }
    int c = url.indexOf(';');
    if (c == -1) {
      c = len;
    }
    int end = Math.min(q, c);
    String clean = url.substring(0, end);
    int start = Math.max(clean.lastIndexOf(':'), clean.lastIndexOf('/'));
    String dbname = clean.substring(start + 1);

    // check parameters
    int pstart = end + 1;
    int pnameStart = pstart;
    int pnameEnd = pstart;
    for (int i = pstart; i < len; i++) {
      char ch = url.charAt(i);
      if (ch == ';' || ch == '&') {
        String key = url.substring(pnameStart, pnameEnd).trim();
        if (key.equalsIgnoreCase("databaseName") || key.equalsIgnoreCase("database")) {
          dbname = url.substring(pnameEnd + 1, i).trim();
          break;
        }
        pnameStart = i + 1;
      } else if (ch == '=') {
        pnameEnd = i;
      }
    }
    return dbname;
  }

  ;

  public static Map<String, Object> defaults(String database, Environment env) {
    Map<String, Object> defaults = new HashMap<>();
    defaults.put("maximumPoolSize",
        Math.max(10, Runtime.getRuntime().availableProcessors() * 2 + 1));
    if (database == null) {
      return defaults;
    }
    switch (database) {
      case "derby": {
        // url => jdbc:derby:${db};create=true
        defaults.put("dataSourceClassName", "org.apache.derby.jdbc.ClientDataSource");
        return defaults;
      }
      case "db2": {
        // url => jdbc:db2://127.0.0.1:50000/SAMPLE
        defaults.put("dataSourceClassName", "com.ibm.db2.jcc.DB2SimpleDataSource");
        return defaults;
      }
      case "h2": {
        //url => mem, fs or jdbc:h2:${db}
        defaults.put("dataSourceClassName", "org.h2.jdbcx.JdbcDataSource");
        defaults.put("dataSource.user", "sa");
        defaults.put("dataSource.password", "");
        return defaults;
      }
      case "hsqldb": {
        // url =>  jdbc:hsqldb:file:${db}
        defaults.put("dataSourceClassName", "org.hsqldb.jdbc.JDBCDataSource");
        return defaults;
      }
      case "mariadb": {
        // url jdbc:mariadb://<host>:<port>/<database>?<key1>=<value1>&<key2>=<value2>...
        defaults.put("dataSourceClassName", "org.mariadb.jdbc.MySQLDataSource");
        return defaults;
      }
      case "mysql": {
        // url jdbc:mysql://<host>:<port>/<database>?<key1>=<value1>&<key2>=<value2>...
        // 6.x
        env.loadClass("com.mysql.cj.jdbc.MysqlDataSource")
            .ifPresent(klass -> defaults.put("dataSourceClassName", klass.getName()));
        // 5.x
        if (!defaults.containsKey("dataSourceClassName")) {
          env.loadClass("com.mysql.jdbc.jdbc2.optional.MysqlDataSource").ifPresent(klass -> {
            defaults.put("dataSourceClassName", klass.getName());
            defaults.put("dataSource.encoding", env.getConfig().getString("application.charset"));
            defaults.put("dataSource.cachePrepStmts", true);
            defaults.put("dataSource.prepStmtCacheSize", 250);
            defaults.put("dataSource.prepStmtCacheSqlLimit", 2048);
            defaults.put("dataSource.useServerPrepStmts", true);
          });
        }
        return defaults;
      }
      case "sqlserver": {
        // url => jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
        defaults.put("dataSourceClassName", "com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        return defaults;
      }
      case "oracle": {
        // url => jdbc:oracle:thin:@//<host>:<port>/<service_name>
        defaults.put("dataSourceClassName", "oracle.jdbc.pool.OracleDataSource");
        return defaults;
      }
      case "pgsql": {
        // url => jdbc:pgsql://<server>[:<port>]/<database>
        defaults.put("dataSourceClassName", "com.impossibl.postgres.jdbc.PGDataSource");
        return defaults;
      }
      case "postgresql": {
        // url => jdbc:postgresql://host:port/database
        defaults.put("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
        return defaults;
      }
      case "sybase": {
        // url => jdbc:jtds:sybase://<host>[:<port>][/<database_name>]
        defaults.put("dataSourceClassName", "com.sybase.jdbcx.SybDataSource");
        return defaults;
      }
      case "firebirdsql": {
        // jdbc:firebirdsql:host[/port]:<database>
        defaults.put("dataSourceClassName", "org.firebirdsql.pool.FBSimpleDataSource");
        return defaults;
      }
      case "sqlite": {
        // jdbc:sqlite:${db}
        defaults.put("dataSourceClassName", "org.sqlite.SQLiteDataSource");
        return defaults;
      }
      case "log4jdbc": {
        // jdbc:log4jdbc:${dbtype}:${db}
        defaults.put("driverClassName", "net.sf.log4jdbc.DriverSpy");
        return defaults;
      }
      default: {
        return defaults;
      }
    }
  }
}
