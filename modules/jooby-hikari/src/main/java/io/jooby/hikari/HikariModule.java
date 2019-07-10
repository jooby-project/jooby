/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hikari;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hikari connection pool module: https://jooby.io/modules/hikari.
 *
 * mySQL Example:
 *
 * application.conf:
 * <pre>{@code
 *  db.url = "jdbc:mysql://localhost/mydb"
 *  db.user = myuser
 *  db.password = mypassword
 * }</pre>
 *
 * App.java:
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 * }
 * }</pre>
 *
 * To simplify development Jooby offers 3 special databases based on H2 Java database engine:
 *
 * - mem: for in-memory database
 * - local: for a file system database stored in the current project directory
 * - tmp: for a file system database stored in the operating system temporary directory
 *
 * To use any of these database you first need to add the h2 driver to your project and then:
 *
 * - define the <code>db</code> property in your application configuration file, like
 *     <code>db=mem</code> or
 * - pass the database type to the HikariModule. <code>new HikariModule("mem")</code>
 *
 * Alternative you can specify a jdbc connection string:
 *
 * <pre>{@code
 *   install(new HikariModule("jdbc:mysql://localhost/mydb"));
 * }</pre>
 *
 * The module exposes a {@link DataSource} instance which can be retrieve it after installing:
 *
 * <pre>{@code
 *
 *    install(new HikariModule("jdbc:mysql://localhost/mydb"));
 *
 *    DataSource dataSource = require(DataSource.class);
 * }</pre>
 *
 * Supports multiple database connections:
 *
 * <pre>{@code
 *  install(new HikariModule("maindb"));
 *
 *  install(new HikariModule("auditdb"));
 *
 *  DataSource maindb = require(DataSource.class, "maindb");
 *
 *  DataSource auditdb = require(DataSource.class, "auditdb");
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/hikari.
 *
 * @author edgar
 * @since 2.0.0
 */
public class HikariModule implements Extension {

  static {
    System.setProperty("log4jdbc.auto.load.popular.drivers", "false");
  }

  private static final Object MYSQL5_STT_CACHE_SIZE = 250;

  private static final Object MYSQL5_STT_CACHE_SQL_LIMIT = 2048;

  private static final String DATASOURCE_CLASS_NAME = "dataSourceClassName";

  private static final String DRIVER_CLASS_NAME = "driverClassName";

  /** Minimum connection pool size. */
  private static final int MINIMUM_SIZE = 10;

  private static final Set<String> SKIP_TOKENS = Stream.of("jdbc", "jtds")
      .collect(Collectors.toSet());

  /** Default datasource key. Used for retrieving the default datasource. */
  public static final ServiceKey<DataSource> KEY = ServiceKey.key(DataSource.class);

  private HikariConfig hikari;

  private String database;

  /**
   * Creates a new Hikari module. The database parameter can be one of:
   *
   * - A property key defined in your application configuration file, like <code>db</code>.
   * - A special h2 database: mem, local or tmp.
   * - A jdbc connection string, like: <code>jdbc:mysql://localhost/db</code>
   *
   * @param database Database key, database type or connection string.
   */
  public HikariModule(@Nonnull String database) {
    this.database = database;
  }

  /**
   * Creates a new Hikari module using the <code>db</code> property key. This key must be
   * present in the application configuration file, like:
   *
   * <pre>{@code
   *  db.url = "jdbc:url"
   *  db.user = dbuser
   *  db.password = dbpass
   * }</pre>
   */
  public HikariModule() {
    this("db");
  }

  /**
   * Creates a new Hikari module using the Hikari configuration.
   *
   * @param hikari Hikari configuration.
   */
  public HikariModule(@Nonnull HikariConfig hikari) {
    this(hikari.getPoolName());
    this.hikari = hikari;
  }

  @Override public void install(@Nonnull Jooby application) {
    if (hikari == null) {
      hikari = build(application.getEnvironment(), database);
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

  /**
   * Get a database type from jdbc url. Examples:
   *
   * - jdbc:mysql://localhost/mydb =&gt; mysql
   * - jdbc:postgresql://server/database =&gt; postgresql
   *
   * @param url Jdbc connection string (a.k.a jdbc url)
   * @return Database type or given jdbc connection string for unknown or bad urls.
   */
  public static @Nonnull String databaseType(@Nonnull String url) {
    String type = Arrays.stream(url.toLowerCase().split(":"))
        .filter(token -> !SKIP_TOKENS.contains(token))
        .findFirst()
        .orElse(url);
    return type;
  }

  /**
   * Get a database name from jdbc url. Examples:
   *
   * - jdbc:mysql://localhost/mydb =&gt; mydb
   * - jdbc:postgresql://server/database =&gt; database
   *
   * @param url Jdbc connection string (a.k.a jdbc url)
   * @return Database name.
   */
  public static @Nonnull String databaseName(@Nonnull String url) {
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

  private static Map<String, Object> defaults(String database, Environment env) {
    Map<String, Object> defaults = new HashMap<>();
    defaults.put("maximumPoolSize",
        Math.max(MINIMUM_SIZE, Runtime.getRuntime().availableProcessors() * 2 + 1));
    if ("derby".equals(database)) {
      // url => jdbc:derby:${db};create=true
      defaults.put("dataSourceClassName", "org.apache.derby.jdbc.ClientDataSource");
    } else if ("db2".equals(database)) {
      // url => jdbc:db2://127.0.0.1:50000/SAMPLE
      defaults.put("dataSourceClassName", "com.ibm.db2.jcc.DB2SimpleDataSource");
    } else if ("h2".equals(database)) {
      //url => mem, fs or jdbc:h2:${db}
      defaults.put("dataSourceClassName", "org.h2.jdbcx.JdbcDataSource");
      defaults.put("dataSource.user", "sa");
      defaults.put("dataSource.password", "");
    } else if ("hsqldb".equals(database)) {
      // url =>  jdbc:hsqldb:file:${db}
      defaults.put("dataSourceClassName", "org.hsqldb.jdbc.JDBCDataSource");
    } else if ("mariadb".equals(database)) {
      // url jdbc:mariadb://<host>:<port>/<database>?<key1>=<value1>&<key2>=<value2>...
      defaults.put("dataSourceClassName", "org.mariadb.jdbc.MySQLDataSource");
    } else if ("mysql".equals(database)) {
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
          defaults.put("dataSource.prepStmtCacheSize", MYSQL5_STT_CACHE_SIZE);
          defaults.put("dataSource.prepStmtCacheSqlLimit", MYSQL5_STT_CACHE_SQL_LIMIT);
          defaults.put("dataSource.useServerPrepStmts", true);
        });
      }
    } else if ("sqlserver".equals(database)) {
      // url => jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
      defaults.put("dataSourceClassName", "com.microsoft.sqlserver.jdbc.SQLServerDataSource");
    } else if ("oracle".equals(database)) {
      // url => jdbc:oracle:thin:@//<host>:<port>/<service_name>
      defaults.put("dataSourceClassName", "oracle.jdbc.pool.OracleDataSource");
    } else if ("pgsql".equals(database)) {
      // url => jdbc:pgsql://<server>[:<port>]/<database>
      defaults.put("dataSourceClassName", "com.impossibl.postgres.jdbc.PGDataSource");
    } else if ("postgresql".equals(database)) {
      // url => jdbc:postgresql://host:port/database
      defaults.put("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    } else if ("sybase".equals(database)) {
      // url => jdbc:jtds:sybase://<host>[:<port>][/<database_name>]
      defaults.put("dataSourceClassName", "com.sybase.jdbcx.SybDataSource");
    } else if ("firebirdsql".equals(database)) {
      // jdbc:firebirdsql:host[/port]:<database>
      defaults.put("dataSourceClassName", "org.firebirdsql.pool.FBSimpleDataSource");
    } else if ("sqlite".equals(database)) {
      // jdbc:sqlite:${db}
      defaults.put("dataSourceClassName", "org.sqlite.SQLiteDataSource");
    } else if ("log4jdbc".equals(database)) {
      // jdbc:log4jdbc:${dbtype}:${db}
      defaults.put("driverClassName", "net.sf.log4jdbc.DriverSpy");
    }
    return defaults;
  }

  static HikariConfig build(Environment env, String database) {
    Properties properties;
    Config config = env.getConfig();
    /**
     * database:
     *  - key
     *  - jdbc url
     *  - special database
     */
    boolean isProperty = isProperty(config, database);
    String dbkey = database;
    if (isProperty) {
      properties = properties(config, database);
    } else {
      properties = jdbcUrl(config, database);
    }
    String dburl = (String) properties.get("dataSource.url");
    String dbtype;
    String dbname;
    if (dburl != null) {
      dbtype = databaseType(dburl);
      dbname = databaseName(dburl);
    } else {
      dbtype = null;
      dbname = null;
    }

    if (dbname != null && !dbkey.equals(dbname)) {
      dumpProperties(config, dbname, "dataSource.", properties::setProperty);
    }

    /** *.dataSource AND *.hikari */
    Stream.of(dbkey, dbname)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(key ->
            dumpProperties(config, "hikari", "", properties::setProperty)
        );
    Stream.of(dbkey, dbname)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(key -> {
          dumpProperties(config, key + ".dataSource", "dataSource.", properties::setProperty);
          dumpProperties(config, key + ".hikari", "", properties::setProperty);
        });

    Map<String, Object> defaults = defaults(dbtype, env);
    Properties configuration = new Properties();
    configuration.putAll(defaults);
    configuration.putAll(properties);

    if (configuration.containsKey(DRIVER_CLASS_NAME)) {
      configuration.remove(DATASOURCE_CLASS_NAME);
      configuration.remove("dataSource.url");
      configuration.setProperty("jdbcUrl", dburl);
    }

    if (dbtype == null) {
      String poolName = Stream.of(
          configuration.getProperty(DATASOURCE_CLASS_NAME),
          configuration.getProperty(DRIVER_CLASS_NAME),
          configuration.getProperty("dataSource.database"),
          configuration.getProperty("dataSource.databaseName")
      )
          .filter(Objects::nonNull)
          .map(n -> n.replace("DataSource", "").replace("Driver", ""))
          .map(n -> {
            int i = n.lastIndexOf('.');
            return i != -1 ? n.substring(i + 1).toLowerCase() : n;
          })
          .collect(Collectors.joining("."));
      configuration.put("poolName", poolName);
    } else {
      configuration.put("poolName", dbtype + "." + dbname);
    }

    Optional.ofNullable(configuration.remove("dataSource.user"))
        .ifPresent(user -> configuration.setProperty("username", user.toString()));
    Optional.ofNullable(configuration.remove("dataSource.password"))
        .ifPresent(password -> configuration.setProperty("password", password.toString()));
    return new HikariConfig(configuration);
  }

  private static void dumpProperties(Config config, String key, String prefix,
      BiConsumer<String, String> consumer) {
    if (isProperty(config, key)) {
      Object anyRef = config.getAnyRef(key);
      if (anyRef instanceof Map) {
        Set<Map.Entry> entries = ((Map) anyRef).entrySet();
        for (Map.Entry e : entries) {
          Object value = e.getValue();
          if (!(value instanceof Map)) {
            String k = prefix + e.getKey();
            consumer.accept(k, value.toString());
          }
        }
      }
    }
  }

  private static Properties properties(Config config, String database) {
    Properties hikari;

    ConfigValue dbvalue = config.getValue(database);
    if (dbvalue.valueType() == ConfigValueType.OBJECT) {
      hikari = new Properties();
      dumpProperties(config, database, "dataSource.", hikari::setProperty);
    } else {
      hikari = jdbcUrl(config, (String) dbvalue.unwrapped());
    }

    // Move dataSourceClassName/driverClassName
    Stream.of(DATASOURCE_CLASS_NAME, DRIVER_CLASS_NAME).forEach(k -> {
      String value = (String) hikari.remove("dataSource." + k);
      if (value != null) {
        hikari.setProperty(k, value);
      }
    });
    return hikari;
  }

  private static boolean isProperty(Config config, String key) {
    try {
      return config.hasPath(key);
    } catch (ConfigException x) {
      return false;
    }
  }

  private static Properties jdbcUrl(Config conf, String database) {
    Properties hikari = new Properties();
    if ("mem".equals(database)) {
      hikari.setProperty("dataSource.url", "jdbc:h2:mem:@mem" + rnd() + ";DB_CLOSE_DELAY=-1");
      hikari.setProperty("dataSource.user", "sa");
      hikari.setProperty("dataSource.password", "");
    } else if ("local".equals(database) || "tmp".equals(database)) {
      String name;
      Path basedir;
      if ("local".equals(database)) {
        basedir = Paths.get(System.getProperty("user.dir"));
        name = basedir.getFileName().toString();
      } else {
        name = "tmp" + rnd();
        basedir = Paths.get(conf.getString("application.tmpdir"));
      }
      Path path = basedir.resolve(name);
      hikari.setProperty("dataSource.url", "jdbc:h2:" + path.toAbsolutePath());
      hikari.setProperty("dataSource.user", "sa");
      hikari.setProperty("dataSource.password", "");
    } else {
      hikari.setProperty("dataSource.url", database);
    }
    return hikari;
  }

  private static String rnd() {
    return Long.toHexString(UUID.randomUUID().getMostSignificantBits());
  }
}
