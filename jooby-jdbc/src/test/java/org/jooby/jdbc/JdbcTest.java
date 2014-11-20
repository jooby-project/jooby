package org.jooby.jdbc;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.function.BiConsumer;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.easymock.Capture;
import org.jooby.Env;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;

public class JdbcTest {

  @SuppressWarnings("unchecked")
  @Test
  public void memdb() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    // config
    config = config.withValue("db", fromAnyRef("mem"));

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.h2.jdbcx.JdbcDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1", properties.get("url"));
        assertEquals("sa", properties.get("user"));
        assertEquals("", properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void fsdb() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dbname = "test";
    String tmpdir = System.getProperty("java.io.tmpdir");
    config = config.withValue("db", fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef(dbname))
        .withValue("application.tmpdir", fromAnyRef(tmpdir))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .resolve();

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.h2.jdbcx.JdbcDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals("jdbc:h2:" + tmpdir + dbname, properties.get("url"));
        assertEquals("sa", properties.get("user"));
        assertEquals("", properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dbPropertyCanBeJustURL() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db", fromAnyRef("jdbc:h2:testdb"));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.h2.jdbcx.JdbcDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals("jdbc:h2:testdb", properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dbHashMustHaveURLwhenMoreDetailsAreProvided() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db.url", fromAnyRef("jdbc:h2:testdb"))
        .withValue("db.user", fromAnyRef("test"));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.h2.jdbcx.JdbcDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals("jdbc:h2:testdb", properties.get("url"));
        assertEquals("test", properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void derby() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db", fromAnyRef("jdbc:derby:testdb"));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("org.apache.derby.jdbc.ClientDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals("jdbc:derby:testdb", properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void db2() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:db2://127.0.0.1:50000/SAMPLE";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("com.ibm.db2.jcc.DB2SimpleDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hsql() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:hsqldb:file";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.hsqldb.jdbc.JDBCDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void mariadb() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:mariadb://localhost/db";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.mariadb.jdbc.MySQLDataSource", hikariConfig.getDataSourceClassName());

      // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void mysql() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:mysql://localhost/db";
    config = config.withValue("db", fromAnyRef(dburl))
        .withValue("application.name", fromAnyRef("test"))
        .withValue("application.tmpdir", fromAnyRef(System.getProperty("java.io.tmpdir")))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .resolve();

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("com.mysql.jdbc.jdbc2.optional.MysqlDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals("UTF-8", properties.get("encoding"));
        assertEquals("true", properties.get("cachePrepStmts"));
        assertEquals("250", properties.get("prepStmtCacheSize"));
        assertEquals("2048", properties.get("prepStmtCacheSqlLimit"));
        assertEquals("true", properties.get("useServerPrepStmts"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dbspecific() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:mysql://localhost/db";
    config = config.withValue("db.url", fromAnyRef(dburl))
        .withValue("db.user", fromAnyRef("test"))
        .withValue("db.password", fromAnyRef("pass"))
        .withValue("application.name", fromAnyRef("test"))
        .withValue("application.tmpdir", fromAnyRef(System.getProperty("java.io.tmpdir")))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        // override defaults
        .withValue("db.cachePrepStmts", fromAnyRef(false))
        .resolve();

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("com.mysql.jdbc.jdbc2.optional.MysqlDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals("UTF-8", properties.get("encoding"));
        assertEquals("false", properties.get("cachePrepStmts"));
        assertEquals("250", properties.get("prepStmtCacheSize"));
        assertEquals("2048", properties.get("prepStmtCacheSqlLimit"));
        assertEquals("true", properties.get("useServerPrepStmts"));
        assertEquals("test", properties.get("user"));
        assertEquals("pass", properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hikariDefaultsDev() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db", fromAnyRef("mem"));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals(null, hikariConfig.getCatalog());
          assertEquals(null, hikariConfig.getConnectionTestQuery());
          assertEquals(null, hikariConfig.getConnectionInitSql());
          assertEquals(30000, hikariConfig.getConnectionTimeout());
          assertEquals(600000, hikariConfig.getIdleTimeout());
          assertEquals(true, hikariConfig.isAutoCommit());
          assertEquals(true, hikariConfig.isInitializationFailFast());
          assertEquals(false, hikariConfig.isIsolateInternalQueries());
          assertEquals(false, hikariConfig.isReadOnly());
          assertEquals(false, hikariConfig.isRegisterMbeans());
          assertEquals(0, hikariConfig.getLeakDetectionThreshold());
          assertEquals(1800000, hikariConfig.getMaxLifetime());
          assertEquals(10, hikariConfig.getMaximumPoolSize());
          assertEquals(-1, hikariConfig.getMinimumIdle());
          assertEquals(null, hikariConfig.getPoolName());
          assertEquals(null, hikariConfig.getTransactionIsolation());
        });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hikariOverrideDefaults() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    long connectionTimeout = 1000;
    int maximumPoolSize = 10;
    long idleTimeout = 2000;
    // config
    config = config.withValue("db", fromAnyRef("mem"))
        // hikari override
        .withValue("hikari.connectionTimeout", fromAnyRef(connectionTimeout))
        .withValue("hikari.maximumPoolSize", fromAnyRef(maximumPoolSize))
        .withValue("hikari.idleTimeout", fromAnyRef(idleTimeout))
        .withValue("hikari.autoCommit", fromAnyRef(false));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals(null, hikariConfig.getCatalog());
          assertEquals(null, hikariConfig.getConnectionTestQuery());
          assertEquals(null, hikariConfig.getConnectionInitSql());
          assertEquals(connectionTimeout, hikariConfig.getConnectionTimeout());
          assertEquals(idleTimeout, hikariConfig.getIdleTimeout());
          assertEquals(false, hikariConfig.isAutoCommit());
          assertEquals(true, hikariConfig.isInitializationFailFast());
          assertEquals(false, hikariConfig.isIsolateInternalQueries());
          assertEquals(false, hikariConfig.isReadOnly());
          assertEquals(false, hikariConfig.isRegisterMbeans());
          assertEquals(0, hikariConfig.getLeakDetectionThreshold());
          assertEquals(1800000, hikariConfig.getMaxLifetime());
          assertEquals(maximumPoolSize, hikariConfig.getMaximumPoolSize());
          assertEquals(-1, hikariConfig.getMinimumIdle());
          assertEquals(null, hikariConfig.getPoolName());
          assertEquals(null, hikariConfig.getTransactionIsolation());
        });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void overrideDataSource() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db", fromAnyRef("mem"))
        .withValue("hikari.dataSourceClassName", fromAnyRef("test.MyDataSource"));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("test.MyDataSource", hikariConfig.getDataSourceClassName());

          assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1", properties.get("url"));
          assertEquals("sa", properties.get("user"));
          assertEquals("", properties.get("password"));
        });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void twoDatabases() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    config = config.withValue("db.audit", fromAnyRef("mem"))
        .withValue("hikari.audit.maximumPoolSize", fromAnyRef(1));

    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class, Names.named("db.audit")))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc("audit").configure(mode, config, binder);

    withHikariConfig(provider, (hikariConfig, properties) -> {
      assertEquals("org.h2.jdbcx.JdbcDataSource", hikariConfig.getDataSourceClassName());
      assertEquals(1, hikariConfig.getMaximumPoolSize());

      // datasource properties
        assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1", properties.get("url"));
        assertEquals("sa", properties.get("user"));
        assertEquals("", properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sqlserver() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:sqlserver://serverName";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void oracle() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:oracle:thin:@//<host>:<port>/<service_name>";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("oracle.jdbc.pool.OracleDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void pgsql() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:pgsql://<server>[:<port>]/<database>";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("com.impossibl.postgres.jdbc.PGDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void postgresql() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:postgresql://host:port/database";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("org.postgresql.ds.PGSimpleDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sybase() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:jtds:sybase://<host>[:<port>][/<database_name>]";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("com.sybase.jdbcx.SybDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void firebirdsql() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:firebirdsql:host[/port]:<database>";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("org.firebirdsql.pool.FBSimpleDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sqlite() throws Exception {
    Env mode = mode("dev");
    Config config = ConfigFactory.parseResources(getClass(), "jdbc.conf");
    Binder binder = createMock(Binder.class);

    // config
    String dburl = "jdbc:sqlite:testdb";
    config = config.withValue("db", fromAnyRef(dburl));

    // binder
    ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);

    Capture<Provider<DataSource>> provider = new Capture<>();
    LinkedBindingBuilder<DataSource> binding = createMock(LinkedBindingBuilder.class);
    expect(binding.toProvider(capture(provider))).andReturn(scope);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);

    Object[] mocks = {binder, binding, scope };

    replay(mocks);

    new Jdbc().configure(mode, config, binder);

    withHikariConfig(
        provider,
        (hikariConfig, properties) -> {
          assertEquals("org.sqlite.SQLiteDataSource",
              hikariConfig.getDataSourceClassName());

          // datasource properties
        assertEquals(dburl, properties.get("url"));
        assertEquals(null, properties.get("user"));
        assertEquals(null, properties.get("password"));
      });

    verify(mocks);
  }

  private static void withHikariConfig(final Capture<Provider<DataSource>> provider,
      final BiConsumer<HikariConfig, Properties> asserts) {
    assertNotNull(provider);
    withHikariConfig(provider.getValue(), asserts);
  }

  private static void withHikariConfig(final Provider<DataSource> provider,
      final BiConsumer<HikariConfig, Properties> asserts) {
    assertNotNull(provider);
    assertTrue(provider instanceof HikariDataSourceProvider);
    HikariDataSourceProvider hikariProvider = (HikariDataSourceProvider) provider;

    HikariConfig hikariConfig = hikariProvider.config();
    assertNotNull(hikariConfig);

    asserts.accept(hikariConfig, hikariConfig.getDataSourceProperties());
  }

  private static Env mode(final String name) {
    return new Env() {

      @Override
      public Config config() {
        return ConfigFactory.empty();
      }

      @Override
      public String name() {
        return name;
      }
    };
  }
}
