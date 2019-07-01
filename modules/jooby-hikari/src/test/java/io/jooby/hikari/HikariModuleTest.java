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

public class HikariModuleTest {

  static int MAX_POOL_SIZE = Math.max(10, Runtime.getRuntime().availableProcessors() * 2 + 1);

  @Test
  public void mem() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), mapOf("db", "mem"), "test"), "db");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getUsername());
    assertEquals("", conf.getPassword());
    assertTrue(Pattern.matches("jdbc:h2:mem:.*;DB_CLOSE_DELAY=-1",
        conf.getDataSourceProperties().getProperty("url")));
  }

  @Test
  public void local() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db", "local", "application.package", "foo", "application.tmpdir", "target"), "test"), "db");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getUsername());
    assertEquals("", conf.getPassword());
    assertTrue(Pattern.matches("jdbc:h2:.*", conf.getDataSourceProperties().getProperty("url")),
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void dbWithCredentials() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "db.user", "root", "db.password", ""), "test"), "db");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("root", conf.getUsername());
    assertEquals("", conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void connectionString() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), mapOf("mydb.user", "root", "mydb.password", ""), "test"),
            "jdbc:mysql://localhost/mydb");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.mydb", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("root", conf.getUsername());
    assertEquals("", conf.getPassword());
    assertEquals("jdbc:mysql://localhost/mydb", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void memConnectionString() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"), "mem");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("org.h2.jdbcx.JdbcDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertTrue(Pattern.matches("h2\\..*", conf.getPoolName()));
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("sa", conf.getUsername());
    assertEquals("", conf.getPassword());
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
            "db.audit.hikari.maximumPoolSize", "1"), "test");
    HikariConfig db = HikariModule.build(env, "db.main");
    assertEquals(5, db.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", db.getDataSourceClassName());
    assertEquals(null, db.getJdbcUrl());
    assertEquals("mysql.main", db.getPoolName());
    assertNotNull(db.getDataSourceProperties());
    assertEquals("m", db.getUsername());
    assertEquals("p1", db.getPassword());
    assertEquals("jdbc:mysql://localhost/main", db.getDataSourceProperties().getProperty("url"));

    db = HikariModule.build(env, "db.audit");
    assertEquals(1, db.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", db.getDataSourceClassName());
    assertEquals(null, db.getJdbcUrl());
    assertEquals("mysql.audit", db.getPoolName());
    assertNotNull(db.getDataSourceProperties());
    assertEquals("a", db.getUsername());
    assertEquals("p2", db.getPassword());
    assertEquals("jdbc:mysql://localhost/audit", db.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void dbUrlWithParams() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url",
                "jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8"), "test"), "db");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db?useEncoding=true&characterEncoding=UTF-8",
        conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariOptions() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "db.hikari.maximumPoolSize", "5"), "test"), "db");
    assertEquals(5, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariDefaultOptions() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.maximumPoolSize", "5"), "test"), "db");
    assertEquals(5, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void hikariOverrideOptions() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.maximumPoolSize", "5",
                "db.hikari.maximumPoolSize", "7"), "test"), "db");
    assertEquals(7, conf.getMaximumPoolSize());
    assertEquals("com.mysql.cj.jdbc.MysqlDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void overrideDataSource() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:mysql://localhost/db", "hikari.dataSourceClassName",
                "test.MyDS"), "test"), "db");
    assertEquals("test.MyDS", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mysql.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void noUrlProperty() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf(
                "db.host", "localhost",
                "db.database", "foo",
                "db.user", "root",
                "db.port", "333",
                "db.dataSourceClassName", "com.impossibl.postgres.jdbc.PGDataSource"), "test"), "db");
    assertEquals("com.impossibl.postgres.jdbc.PGDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("pg.foo", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("foo", conf.getDataSourceProperties().getProperty("database"));
    assertEquals("localhost", conf.getDataSourceProperties().getProperty("host"));
    assertEquals("333", conf.getDataSourceProperties().getProperty("port"));
    assertEquals("root", conf.getUsername());
  }

  @Test
  public void log4jdbc() {
    HikariConfig conf = HikariModule
        .build(new Environment(getClass().getClassLoader(), 
            mapOf("db.url", "jdbc:log4jdbc:mysql://localhost/db"), "test"), "db");
    assertEquals(MAX_POOL_SIZE, conf.getMaximumPoolSize());
    assertEquals(null, conf.getDataSourceClassName());
    assertEquals("net.sf.log4jdbc.DriverSpy", conf.getDriverClassName());
    //assertEquals("jdbc:log4jdbc:mysql://localhost/db", conf.getDataSourceProperties().getProperty("url"));
    assertEquals("log4jdbc.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals(null, conf.getUsername());
    assertEquals(null, conf.getPassword());
    assertEquals("jdbc:log4jdbc:mysql://localhost/db", conf.getJdbcUrl());// conf.getDataSourceProperties().getProperty("url"));
  }

  @Test
  public void databaseName() {
    assertEquals("123", HikariModule.databaseName("jdbc:h2:mem:123;DB_CLOSE_DELAY=-1"));
    assertEquals("jdbctest", HikariModule.databaseName("jdbc:h2:target/jdbctest"));
    assertEquals("db", HikariModule.databaseName("jdbc:mysql://localhost/db"));
    assertEquals("testdb", HikariModule.databaseName("jdbc:derby:testdb"));
    assertEquals("SAMPLE", HikariModule.databaseName("jdbc:db2://127.0.0.1:50000/SAMPLE"));
    assertEquals("file", HikariModule.databaseName("jdbc:hsqldb:file"));
    assertEquals("dba", HikariModule.databaseName("jdbc:mariadb://localhost/dba"));
    assertEquals("dbb", HikariModule.databaseName("jdbc:log4jdbc:mysql://localhost/dbb"));
    assertEquals("dbc",
        HikariModule.databaseName("jdbc:mysql://localhost/dbc?useEncoding=true&characterEncoding=UTF-8"));
    assertEquals("AdventureWorks", HikariModule.databaseName(
        "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;"));
    assertEquals("AdventureWorks", HikariModule.databaseName(
        "jdbc:sqlserver://localhost:1433;database=AdventureWorks;integratedSecurity=true;"));
    assertEquals("AdventureWorks",
        HikariModule.databaseName("jdbc:sqlserver://localhost:1433;fpp;databaseName=AdventureWorks;;"));
    assertEquals("orcl", HikariModule.databaseName("jdbc:oracle:thin:@myhost:1521:orcl"));
    assertEquals("database", HikariModule.databaseName("jdbc:pgsql://server/database"));
    assertEquals("database", HikariModule.databaseName("jdbc:postgresql://server/database"));
    assertEquals("database", HikariModule.databaseName("jdbc:jtds:sybase://server/database"));
    assertEquals("mydb", HikariModule.databaseName("jdbc:firebirdsql:host:mydb"));
    assertEquals("testdb", HikariModule.databaseName("jdbc:sqlite:testdb"));
    assertEquals("testdb", HikariModule.databaseName("jdbc:unknown:testdb"));
  }

  @Test
  public void databaseType() {
    assertEquals("h2", HikariModule.databaseType("jdbc:h2:mem:123;DB_CLOSE_DELAY=-1"));
    assertEquals("h2", HikariModule.databaseType("jdbc:h2:target/jdbctest"));
    assertEquals("mysql", HikariModule.databaseType("jdbc:mysql://localhost/db"));
    assertEquals("derby", HikariModule.databaseType("jdbc:derby:testdb"));
    assertEquals("db2", HikariModule.databaseType("jdbc:db2://127.0.0.1:50000/SAMPLE"));
    assertEquals("hsqldb", HikariModule.databaseType("jdbc:hsqldb:file"));
    assertEquals("mariadb", HikariModule.databaseType("jdbc:mariadb://localhost/dba"));
    assertEquals("log4jdbc", HikariModule.databaseType("jdbc:log4jdbc:mysql://localhost/dbb"));
    assertEquals("mysql",
        HikariModule.databaseType("jdbc:mysql://localhost/dbc?useEncoding=true&characterEncoding=UTF-8"));
    assertEquals("sqlserver", HikariModule.databaseType(
        "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;"));
    assertEquals("sqlserver", HikariModule.databaseType(
        "jdbc:sqlserver://localhost:1433;database=AdventureWorks;integratedSecurity=true;"));
    assertEquals("sqlserver",
        HikariModule.databaseType("jdbc:sqlserver://localhost:1433;fpp;databaseName=AdventureWorks;;"));
    assertEquals("oracle", HikariModule.databaseType("jdbc:oracle:thin:@myhost:1521:orcl"));
    assertEquals("pgsql", HikariModule.databaseType("jdbc:pgsql://server/database"));
    assertEquals("postgresql", HikariModule.databaseType("jdbc:postgresql://server/database"));
    assertEquals("sybase", HikariModule.databaseType("jdbc:jtds:sybase://server/database"));
    assertEquals("firebirdsql", HikariModule.databaseType("jdbc:firebirdsql:host:mydb"));
    assertEquals("sqlite", HikariModule.databaseType("jdbc:sqlite:testdb"));
    assertEquals("unknown", HikariModule.databaseType("jdbc:unknown:testdb"));
    assertEquals("foo", HikariModule.databaseType("foo"));
  }

  private Config mapOf(String... values) {
    Map<String, String> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return ConfigFactory.parseMap(hash);
  }
}
