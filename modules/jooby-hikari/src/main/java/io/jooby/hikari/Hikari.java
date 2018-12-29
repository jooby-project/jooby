package io.jooby.hikari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jooby.Env;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Value;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Hikari implements Extension {

  public static class Builder {

    public HikariConfig build(Env env) {
      return build(env, "db");
    }

    public HikariConfig build(Env env, String database) {
      String dburl, dbkey;
      database = builtindb(env, database);
      if (database.startsWith("jdbc:")) {
        dbkey = databaseName(database);
        dburl = database;
      } else {
        Value db = env.get(database);
        if (db.isSimple()) {
          dburl = db.value();
        } else {
          dburl = env.get(database + ".url").value((String) null);
        }
        dbkey = database;
      }

      return build(env, dbkey, builtindb(env, dburl));
    }

    private String builtindb(Env env, String database) {
      if ("mem".equals(database)) {
        return "jdbc:h2:mem:" + System.currentTimeMillis() + ";DB_CLOSE_DELAY=-1";
      } else if ("fs".equals(database)) {
        Path path = Paths
            .get(env.get("application.tmpdir").value(), env.get("application.name").value());
        return "jdbc:h2:" + path.toAbsolutePath();
      }
      return database;
    }

    private HikariConfig build(Env env, String dbkey, String dburl) {
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

    private void props(Env env, BiConsumer<String, String> consumer, String... keys) {
      for (String key : keys) {
        int dot = key.lastIndexOf('.');
        String prefix = (dot > 0 ? key.substring(dot + 1) : key) + ".";
        env.get(key).toMap().forEach((k, v) -> {
          if (k.startsWith(prefix)) {
            String leaf = k.substring(prefix.length());
            if (leaf.indexOf('.') == -1) {
              consumer.accept(leaf, v);
            }
          }
        });
      }
    }
  }

  static {
    System.setProperty("log4jdbc.auto.load.popular.drivers", "false");
  }

  private static final String DATASOURCE_CLASS_NAME = "dataSourceClassName";

  private static final Set<String> SKIP_TOKENS = Set.of("jdbc", "jtds");

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
      hikari = builder().build(application.environment(), database);
    }
    HikariDataSource dataSource = new HikariDataSource(hikari);
    application.addService(DataSource.class, database, dataSource);
    application.onStop(dataSource::close);
  }

  public static Builder builder() {
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

  public static Map<String, Object> defaults(String database, Env env) {
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
        dataSourceClass("com.mysql.cj.jdbc.MysqlDataSource", name -> {
          defaults.put("dataSourceClassName", name);
        });
        // 5.x
        dataSourceClass("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", name -> {
          defaults.put("dataSourceClassName", name);
          defaults.put("dataSource.encoding", env.get("charset").value("UTF-8"));
          defaults.put("dataSource.cachePrepStmts", true);
          defaults.put("dataSource.prepStmtCacheSize", 250);
          defaults.put("dataSource.prepStmtCacheSqlLimit", 2048);
          defaults.put("dataSource.useServerPrepStmts", true);
        });
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

  private static void dataSourceClass(String name, Consumer<String> consumer) {
    try {
      consumer.accept(Hikari.class.getClassLoader().loadClass(name).getName());
    } catch (ClassNotFoundException e) {
      // ignore
    }
  }
}
