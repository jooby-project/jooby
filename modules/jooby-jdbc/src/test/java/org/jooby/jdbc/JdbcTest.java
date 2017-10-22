package org.jooby.jdbc;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.easymock.EasyMock.expect;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.sql.DataSource;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jdbc.class, Properties.class, HikariConfig.class, HikariDataSource.class,
    System.class})
public class JdbcTest {

  static String POOL_SIZE = "12";

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  private Block mysql = unit -> {
    Properties props = unit.get(Properties.class);
    expect(props.setProperty("dataSource.useServerPrepStmts", "true")).andReturn(null);
    expect(props.setProperty("dataSource.prepStmtCacheSqlLimit", "2048")).andReturn(null);
    expect(props.setProperty("dataSource.cachePrepStmts", "true")).andReturn(null);
    expect(props.setProperty("dataSource.prepStmtCacheSize", "250")).andReturn(null);
    expect(props.setProperty("dataSource.encoding", "UTF-8")).andReturn(null);
  };

  @Test(expected = IllegalArgumentException.class)
  public void nullname() throws Exception {
    new Jdbc(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyname() throws Exception {
    new Jdbc("");
  }

  @Test
  public void memdb() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(currentTimeMillis(123))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:mem:123;DB_CLOSE_DELAY=-1", "h2.123",
            "sa", "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "h2"))
        .expect(serviceKey("123"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void minpoolsize() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(2))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(currentTimeMillis(123))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:mem:123;DB_CLOSE_DELAY=-1", "h2.123",
            "sa", "", false, false))
        .expect(unit -> {
          Properties props = unit.get(Properties.class);
          expect(props.setProperty("maximumPoolSize", "10")).andReturn(null);
        })
        .expect(hikariConfig(2, null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "h2"))
        .expect(serviceKey("123"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void fsdb() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "h2"))
        .expect(serviceKey("jdbctest"))
        .expect(onStop)
        .expect(unit -> {
          unit.get(HikariDataSource.class).close();
        })
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void cceExceptionInSource() throws Exception {
    ClassCastException cce = new ClassCastException();
    StackTraceElement e = new StackTraceElement(Jdbc.class.getName(), "accept", null, 0);
    cce.setStackTrace(new StackTraceElement[]{e});
    Jdbc.CCE.apply(cce);
  }

  @Test
  public void cceExceptionWithoutSource() throws Exception {
    ClassCastException cce = new ClassCastException();
    StackTraceElement e = new StackTraceElement(JdbcTest.class.getName(), "accept", null, 0);
    cce.setStackTrace(new StackTraceElement[]{e});
    Jdbc.CCE.apply(cce);
  }

  @Test
  public void dbWithCallback() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(serviceKey("db", "h2"))
        .expect(onStop)
        .expect(unit -> {
          HikariConfig h = unit.get(HikariConfig.class);
          h.setAllowPoolSuspension(true);
        })
        .run(unit -> {
          new Jdbc()
              .doWith((final HikariConfig h) -> {
                h.setAllowPoolSuspension(true);
              })
              .configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void databaseWithCredentials() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db.url",
        ConfigValueFactory.fromAnyRef("jdbc:mysql://localhost/db"))
        .withValue("db.user", fromAnyRef("foo"))
        .withValue("db.password", fromAnyRef("bar"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", "jdbc:mysql://localhost/db",
            "mysql.db", "foo", "bar", false))
        .expect(mysql)
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "mysql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void derby() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("jdbc:derby:testdb"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.apache.derby.jdbc.ClientDataSource", "jdbc:derby:testdb", "derby.testdb",
            null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("testdb"))
        .expect(serviceKey("db", "derby"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void connectionString() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf")
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.apache.derby.jdbc.ClientDataSource", null, "derby.testdb",
            null, "", false))
        .expect(hikariConfig(null))
        .expect(unit -> {
          Properties props = unit.mock(Properties.class);
          expect(props.setProperty("url", "jdbc:derby:testdb")).andReturn(null);

          HikariConfig hconf = unit.get(HikariConfig.class);
          expect(hconf.getDataSourceProperties()).andReturn(props);
        })
        .expect(hikariDataSource())
        .expect(serviceKey("testdb", "derby"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc("jdbc:derby:testdb").configure(unit.get(Env.class), config,
              unit.get(Binder.class));
        });
  }

  @Test
  public void db2() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:db2://127.0.0.1:50000/SAMPLE"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("com.ibm.db2.jcc.DB2SimpleDataSource", "jdbc:db2://127.0.0.1:50000/SAMPLE",
            "db2.SAMPLE", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("SAMPLE"))
        .expect(serviceKey("db", "db2"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void hsql() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:hsqldb:file"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.hsqldb.jdbc.JDBCDataSource", "jdbc:hsqldb:file",
            "hsqldb.file", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("file"))
        .expect(serviceKey("db", "hsqldb"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void mariadb() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:mariadb://localhost/db"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.mariadb.jdbc.MySQLDataSource", "jdbc:mariadb://localhost/db",
            "mariadb.db", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "mariadb"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });

  }

  @Test
  public void mysql() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:mysql://localhost/db"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", "jdbc:mysql://localhost/db",
            "mysql.db", null, "", false))
        .expect(mysql)
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "mysql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void dbspecific() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db.url",
        ConfigValueFactory
            .fromAnyRef("jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        // override defaults
        .withValue("db.cachePrepStmts", fromAnyRef(false))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("com.mysql.jdbc.jdbc2.optional.MysqlDataSource",
            "jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8",
            "mysql.db", null, "", false))
        .expect(mysql)
        .expect(unit -> {
          Properties props = unit.get(Properties.class);
          expect(props.setProperty("dataSource.cachePrepStmts", "false")).andReturn(null);
        })
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("db", "mysql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void setHikariOptions() throws Exception {
    long connectionTimeout = 1000;
    int maximumPoolSize = 12;
    long idleTimeout = 800000;

    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("hikari.connectionTimeout", fromAnyRef(connectionTimeout))
        .withValue("hikari.maximumPoolSize", fromAnyRef(maximumPoolSize))
        .withValue("hikari.idleTimeout", fromAnyRef(idleTimeout))
        .withValue("hikari.autoCommit", fromAnyRef(false))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false, false))
        .expect(unit -> {
          Properties props = unit.get(Properties.class);
          expect(props.setProperty("maximumPoolSize", "12")).andReturn(null);
          expect(props.setProperty("maximumPoolSize", "12")).andReturn(null);
          expect(props.setProperty("connectionTimeout", "1000")).andReturn(null);
          expect(props.setProperty("idleTimeout", "800000")).andReturn(null);
          expect(props.setProperty("autoCommit", "false")).andReturn(null);
        })
        .expect(hikariConfig(12))
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(serviceKey("db", "h2"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void overrideDataSource() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("hikari.dataSourceClassName", fromAnyRef("test.MyDataSource"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", true))
        .expect(unit -> {
          Properties properties = unit.get(Properties.class);
          expect(properties.setProperty("dataSourceClassName", "test.MyDataSource"))
              .andReturn(null);
        })
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(serviceKey("db", "h2"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void twoDatabases() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db.audit.url",
        ConfigValueFactory.fromAnyRef("jdbc:h2:mem:audit;DB_CLOSE_DELAY=-1"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("db.audit.user", fromAnyRef("sa"))
        .withValue("db.audit.password", fromAnyRef(""))
        .withValue("db.audit.hikari.dataSourceClassName", fromAnyRef("test.MyDataSource"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(
            props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:mem:audit;DB_CLOSE_DELAY=-1", "h2.audit",
                "sa", "", true))
        .expect(unit -> {
          Properties properties = unit.get(Properties.class);
          expect(properties.setProperty("dataSourceClassName", "test.MyDataSource"))
              .andReturn(null);
        })
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("audit"))
        .expect(serviceKey("db.audit", "h2"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc("db.audit").configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void sqlserver() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef(
            "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(
            props("com.microsoft.sqlserver.jdbc.SQLServerDataSource",
                "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;",
                "sqlserver.AdventureWorks", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("AdventureWorks"))
        .expect(serviceKey("db", "sqlserver"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void oracle() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:oracle:thin:@myhost:1521:orcl"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();
    ;

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("oracle.jdbc.pool.OracleDataSource", "jdbc:oracle:thin:@myhost:1521:orcl",
            "oracle.orcl", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("orcl"))
        .expect(serviceKey("db", "oracle"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void pgsql() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    String url = "jdbc:pgsql://server/database";
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef(url))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(
            props("com.impossibl.postgres.jdbc.PGDataSourceWithUrl", "jdbc:pgsql://server/database",
                "pgsql.database", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("database"))
        .expect(serviceKey("db", "pgsql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void postgresql() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    String url = "jdbc:postgresql://server/database";
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef(url))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.postgresql.ds.PGSimpleDataSource", "jdbc:postgresql://server/database",
            "postgresql.database", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("database"))
        .expect(serviceKey("db", "postgresql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void sybase() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config
        .withValue("db", ConfigValueFactory.fromAnyRef("jdbc:jtds:sybase://server/database"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("com.sybase.jdbcx.SybDataSource", "jdbc:jtds:sybase://server/database",
            "sybase.database", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("database"))
        .expect(serviceKey("db", "sybase"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void firebirdsql() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:firebirdsql:host:mydb"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.firebirdsql.pool.FBSimpleDataSource", "jdbc:firebirdsql:host:mydb",
            "firebirdsql.mydb", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("mydb"))
        .expect(serviceKey("db", "firebirdsql"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void sqlite() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config.withValue("db",
        ConfigValueFactory.fromAnyRef("jdbc:sqlite:testdb"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.sqlite.SQLiteDataSource", "jdbc:sqlite:testdb",
            "sqlite.testdb", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("testdb"))
        .expect(serviceKey("db", "sqlite"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void unknownDb() throws Exception {
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Config dbconf = config
        .withValue("db", ConfigValueFactory.fromAnyRef("jdbc:custom:testdb"))
        .withValue("databases.custom.dataSourceClassName",
            ConfigValueFactory.fromAnyRef("custom.DS"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("runtime.processors-x2", fromAnyRef(POOL_SIZE))
        .resolve();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("custom.DS", "jdbc:custom:testdb",
            "custom.testdb", null, "", false))
        .expect(hikariConfig(null))
        .expect(hikariDataSource())
        .expect(serviceKey("testdb"))
        .expect(serviceKey("db", "custom"))
        .expect(onStop)
        .run(unit -> {
          new Jdbc().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  private Block serviceKey(final String db) {
    return serviceKey(db, null);
  }

  @SuppressWarnings("unchecked")
  private Block serviceKey(final String db, String dbtype) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(new Env.ServiceKey());

      AnnotatedBindingBuilder<DataSource> binding = unit.mock(AnnotatedBindingBuilder.class);
      binding.toInstance(unit.get(HikariDataSource.class));
      binding.toInstance(unit.get(HikariDataSource.class));

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
      expect(binder.bind(Key.get(DataSource.class, Names.named(db)))).andReturn(binding);
      expect(env.set(Key.get(DataSource.class), unit.get(HikariDataSource.class))).andReturn(env);
      expect(env.set(Key.get(DataSource.class, Names.named(db)), unit.get(HikariDataSource.class)))
          .andReturn(env);
      if (dbtype != null) {
        expect(env.set(Key.get(String.class, Names.named(db + ".dbtype")), dbtype)).andReturn(env);
      }
    };
  }

  private Block hikariConfig(Object poolsize) {
    return hikariConfig(Integer.parseInt(POOL_SIZE) + 1, poolsize);
  }

  private Block hikariConfig(Integer defpoolsize, Object poolsize) {
    return unit -> {
      Properties properties = unit.get(Properties.class);
      if (poolsize == null) {
        if (defpoolsize < 10) {
          expect(properties.getOrDefault("maximumPoolSize", "10"))
              .andReturn("10");
        } else {
          expect(properties.getOrDefault("maximumPoolSize", defpoolsize.toString()))
              .andReturn(POOL_SIZE);
        }
      } else {
        expect(properties.getOrDefault("maximumPoolSize", defpoolsize.toString()))
            .andReturn(poolsize);
      }
      HikariConfig hikari = unit.constructor(HikariConfig.class)
          .build(properties);
      unit.registerMock(HikariConfig.class, hikari);
    };
  }

  private Block hikariDataSource() {
    return unit -> {
      HikariConfig properties = unit.get(HikariConfig.class);
      HikariDataSource hikari = unit.constructor(HikariDataSource.class)
          .build(properties);
      unit.registerMock(HikariDataSource.class, hikari);
    };
  }

  private Block currentTimeMillis(final long millis) {
    return unit -> {
      unit.mockStatic(System.class);
      expect(System.currentTimeMillis()).andReturn(millis);
    };
  }

  private Block props(final String dataSourceClassName, final String url, final String name,
      final String username, final String password, final boolean hasDataSourceClassName) {
    return props(dataSourceClassName, url, name, username, password, hasDataSourceClassName, true);
  }

  private Block props(final String dataSourceClassName, final String url, final String name,
      final String username, final String password, final boolean hasDataSourceClassName,
      final boolean poolSize) {
    return unit -> {
      Properties properties = unit.constructor(Properties.class)
          .build();

      expect(properties
          .setProperty("dataSource.dataSourceClassName", dataSourceClassName))
          .andReturn(null);
      if (username != null) {
        expect(properties
            .setProperty("dataSource.user", username))
            .andReturn(null);
        expect(properties
            .setProperty("dataSource.password", password))
            .andReturn(null);
      }
      if (url != null) {
        expect(properties
            .setProperty("dataSource.url", url))
            .andReturn(null);
      }

      if (hasDataSourceClassName) {
        expect(properties.getProperty("dataSourceClassName")).andReturn(dataSourceClassName);
      } else {
        expect(properties.getProperty("dataSourceClassName")).andReturn(null);
        expect(properties.getProperty("dataSource.dataSourceClassName"))
            .andReturn(dataSourceClassName);
        expect(properties.setProperty("dataSourceClassName", dataSourceClassName)).andReturn(null);
      }
      expect(properties.remove("dataSource.dataSourceClassName")).andReturn(dataSourceClassName);
      expect(properties.setProperty("poolName", name)).andReturn(null);
      if (poolSize) {
        expect(properties.setProperty("maximumPoolSize", POOL_SIZE)).andReturn(null);
      }

      unit.registerMock(Properties.class, properties);
    };
  }
}
