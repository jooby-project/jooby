package io.jooby.hikari;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import io.jooby.Environment;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariTest {

  static int MAX_POOL_SIZE = Math.max(10, Runtime.getRuntime().availableProcessors() * 2 + 1);

  @Test
  public void mem() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), mapOf("db", "mem"), "test"));
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getDataSourceProperties().getProperty("user"));
    assertEquals("", conf.getDataSourceProperties().getProperty("password"));
    assertTrue(Pattern.matches("jdbc:h2:mem:.*;DB_CLOSE_DELAY=-1",
        conf.getDataSourceProperties().getProperty("url")));
  }

  @Test
  public void fs() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db", "fs", "application.name", "foo", "application.tmpdir", "target"), "test"));
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getDataSourceProperties().getProperty("user"));
    assertEquals("", conf.getDataSourceProperties().getProperty("password"));
    assertTrue(Pattern.matches("jdbc:h2:.*", conf.getDataSourceProperties().getProperty("url")),
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void dbWithCredentials() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "db.user", "root", "db.password", ""), "test"));
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("root", conf.getDataSourceProperties().getProperty("user"));
    assertEquals("", conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void connectionString() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), mapOf("mydb.user", "root", "mydb.password", ""), "test"),
            "jdbc:mysql://localhost/mydb");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.mydb", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("root", conf.getDataSourceProperties().getProperty("user"));
    assertEquals("", conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/mydb", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void memConnectionString() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"), "mem");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getDataSourceProperties().getProperty("user"));
    assertEquals("", conf.getDataSourceProperties().getProperty("password"));
    assertTrue(Pattern.matches("jdbc:h2:.*", conf.getDataSourceProperties().getProperty("url")),
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void multipledb() {
    Environment env = new Environment(getClass().getClassLoader(), 
        mapOf("db.main.url", "jdbc:mysql://localhost/main", "db.main.user", "m",
            "db.main.password", "p1",
            "db.audit.url", "jdbc:mysql://localhost/audit", "db.audit.user", "a",
            "db.audit.password", "p2",
            "hikari.maximumPoolSize", "5",
            "hikari.audit.maximumPoolSize", "1"), "test");
    HikariConfig db = Hikari.create().build(env, "db.main");
    assertEquals(5, db.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", db.getDataSourceClassName());
    assertEquals(null, db.getJdbcUrl());
    assertEquals("mysql.main", db.getPoolName());
    assertNotNull(db.getDataSourceProperties());
    assertEquals("m", db.getDataSourceProperties().getProperty("user"));
    assertEquals("p1", db.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/main", db.getDataSourceProperties().getProperty("url"));

    db = Hikari.create().build(env, "db.audit");
    assertEquals(1, db.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", db.getDataSourceClassName());
    assertEquals(null, db.getJdbcUrl());
    assertEquals("mysql.audit", db.getPoolName());
    assertNotNull(db.getDataSourceProperties());
    assertEquals("a", db.getDataSourceProperties().getProperty("user"));
    assertEquals("p2", db.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/audit", db.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void dbUrlWithParams() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url",
                "jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8"), "test"));
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8",
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariOptions() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.db.maximumPoolSize", "5"), "test"));
    assertEquals(5, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariDefaultOptions() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.maximumPoolSize", "5"), "test"));
    assertEquals(5, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariOverrideOptions() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.maximumPoolSize", "5",
                "hikari.db.maximumPoolSize", "7"), "test"));
    assertEquals(7, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void overrideDataSource() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.dataSourceClassName",
                "test.MyDS"), "test"));
    assertEquals("test.MyDS", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void noUrlProperty() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf(
                "db.host", "localhost",
                "db.database", "foo",
                "db.user", "root",
                "db.port", "333",
                "db.dataSourceClassName", "com.impossibl.postgres.jdbc.PGDataSource"), "test"));
    assertEquals("com.impossibl.postgres.jdbc.PGDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("pg.foo", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("foo", conf.getDataSourceProperties().getProperty("database"));
    assertEquals("localhost", conf.getDataSourceProperties().getProperty("host"));
    assertEquals("333", conf.getDataSourceProperties().getProperty("port"));
    assertEquals("root", conf.getDataSourceProperties().getProperty("user"));
  }

  @Test
  public void log4jdbc() {
    HikariConfig conf = Hikari.create()
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:log4jdbc:mysql://localhost/db"), "test"));
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals(null, conf.getDataSourceClassName());
    assertEquals("net.sf.log4jdbc.DriverSpy", conf.getDriverClassName());
    assertEquals("jdbc:log4jdbc:mysql://localhost/db", conf.getJdbcUrl());
    assertEquals("log4jdbc.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getDataSourceProperties().getProperty("user"));
    assertEquals(null, conf.getDataSourceProperties().getProperty("password"));
    assertEquals("jdbc:log4jdbc:mysql://localhost/db",
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void databaseName() {
    assertEquals("123", Hikari.databaseName("jdbc:h2:mem:123;DB_CLOSE_DELAY=-1"));
    assertEquals("jdbctest", Hikari.databaseName("jdbc:h2:target/jdbctest"));
    assertEquals("db", Hikari.databaseName("jdbc:mysql://localhost/db"));
    assertEquals("testdb", Hikari.databaseName("jdbc:derby:testdb"));
    assertEquals("SAMPLE", Hikari.databaseName("jdbc:db2://127.0.0.1:50000/SAMPLE"));
    assertEquals("file", Hikari.databaseName("jdbc:hsqldb:file"));
    assertEquals("dba", Hikari.databaseName("jdbc:mariadb://localhost/dba"));
    assertEquals("dbb", Hikari.databaseName("jdbc:log4jdbc:mysql://localhost/dbb"));
    assertEquals("dbc",
        Hikari.databaseName("jdbc:mysql://localhost/dbc?useEncoding=true&characterEncoding=UTF-8"));
    assertEquals("AdventureWorks", Hikari.databaseName(
        "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;"));
    assertEquals("AdventureWorks", Hikari.databaseName(
        "jdbc:sqlserver://localhost:1433;database=AdventureWorks;integratedSecurity=true;"));
    assertEquals("AdventureWorks",
        Hikari.databaseName("jdbc:sqlserver://localhost:1433;fpp;databaseName=AdventureWorks;;"));
    assertEquals("orcl", Hikari.databaseName("jdbc:oracle:thin:@myhost:1521:orcl"));
    assertEquals("database", Hikari.databaseName("jdbc:pgsql://server/database"));
    assertEquals("database", Hikari.databaseName("jdbc:postgresql://server/database"));
    assertEquals("database", Hikari.databaseName("jdbc:jtds:sybase://server/database"));
    assertEquals("mydb", Hikari.databaseName("jdbc:firebirdsql:host:mydb"));
    assertEquals("testdb", Hikari.databaseName("jdbc:sqlite:testdb"));
    assertEquals("testdb", Hikari.databaseName("jdbc:unknown:testdb"));
  }

  @Test
  public void databaseType() {
    assertEquals("h2", Hikari.databaseType("jdbc:h2:mem:123;DB_CLOSE_DELAY=-1"));
    assertEquals("h2", Hikari.databaseType("jdbc:h2:target/jdbctest"));
    assertEquals("mysql", Hikari.databaseType("jdbc:mysql://localhost/db"));
    assertEquals("derby", Hikari.databaseType("jdbc:derby:testdb"));
    assertEquals("db2", Hikari.databaseType("jdbc:db2://127.0.0.1:50000/SAMPLE"));
    assertEquals("hsqldb", Hikari.databaseType("jdbc:hsqldb:file"));
    assertEquals("mariadb", Hikari.databaseType("jdbc:mariadb://localhost/dba"));
    assertEquals("log4jdbc", Hikari.databaseType("jdbc:log4jdbc:mysql://localhost/dbb"));
    assertEquals("mysql",
        Hikari.databaseType("jdbc:mysql://localhost/dbc?useEncoding=true&characterEncoding=UTF-8"));
    assertEquals("sqlserver", Hikari.databaseType(
        "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;"));
    assertEquals("sqlserver", Hikari.databaseType(
        "jdbc:sqlserver://localhost:1433;database=AdventureWorks;integratedSecurity=true;"));
    assertEquals("sqlserver",
        Hikari.databaseType("jdbc:sqlserver://localhost:1433;fpp;databaseName=AdventureWorks;;"));
    assertEquals("oracle", Hikari.databaseType("jdbc:oracle:thin:@myhost:1521:orcl"));
    assertEquals("pgsql", Hikari.databaseType("jdbc:pgsql://server/database"));
    assertEquals("postgresql", Hikari.databaseType("jdbc:postgresql://server/database"));
    assertEquals("sybase", Hikari.databaseType("jdbc:jtds:sybase://server/database"));
    assertEquals("firebirdsql", Hikari.databaseType("jdbc:firebirdsql:host:mydb"));
    assertEquals("sqlite", Hikari.databaseType("jdbc:sqlite:testdb"));
    assertEquals("unknown", Hikari.databaseType("jdbc:unknown:testdb"));
    assertEquals("foo", Hikari.databaseType("foo"));
  }

  private Config mapOf(String... values) {
    Map<String, String> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return ConfigFactory.parseMap(hash);
  }
}
