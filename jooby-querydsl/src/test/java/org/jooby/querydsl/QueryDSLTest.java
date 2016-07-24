package org.jooby.querydsl;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.querydsl.sql.CUBRIDTemplates;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.FirebirdTemplates;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLCloseListener;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.SQLiteTemplates;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueryDSL.class, Configuration.class, H2Templates.class })
public class QueryDSLTest {

  private Block closeconf = unit -> {
    Configuration conf = unit.get(Configuration.class);
    conf.addListener(SQLCloseListener.DEFAULT);
  };

  private MockUnit.Block managed = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
  };

  private Block newSQLQueryFactory = unit -> {
    SQLQueryFactory factory = unit.constructor(SQLQueryFactory.class)
        .build(unit.get(Configuration.class), unit.get(HikariDataSource.class));
    unit.registerMock(SQLQueryFactory.class, factory);
  };

  @Test
  public void defaults() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "jdbctest"))
        .expect(conf(H2Templates.class))
        .expect(bindconf(null))
        .expect(bindconf("jdbctest"))
        .expect(newSQLQueryFactory)
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("jdbctest"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void dbProp() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "jdbctest"))
        .expect(conf(H2Templates.class))
        .expect(bindconf(null))
        .expect(bindconf("jdbctest"))
        .expect(newSQLQueryFactory)
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("jdbctest"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL("db")
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void with() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(sqlTemplate(CUBRIDTemplates.class, null))
        .expect(sqlTemplate(CUBRIDTemplates.class, "jdbctest"))
        .expect(conf(CUBRIDTemplates.class))
        .expect(newSQLQueryFactory)
        .expect(bindconf(null))
        .expect(bindconf("jdbctest"))
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("jdbctest"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .with(new CUBRIDTemplates())
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "jdbctest"))
        .expect(conf(H2Templates.class))
        .expect(newSQLQueryFactory)
        .expect(closeconf)
        .expect(bindconf(null))
        .expect(bindconf("jdbctest"))
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("jdbctest"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .doWith(conf -> {
                conf.addListener(SQLCloseListener.DEFAULT);
              })
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void newSQLFactory() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "jdbctest"))
        .expect(conf(H2Templates.class))
        .expect(newSQLQueryFactory)
        .expect(bindconf(null))
        .expect(bindconf("jdbctest"))
        .expect(sqlqueryfactory())
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("jdbctest"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void db2() {
    assertEquals(DB2Templates.class, QueryDSL.toSQLTemplates("db2").getClass());
  }

  @Test
  public void mysql() {
    assertEquals(MySQLTemplates.class, QueryDSL.toSQLTemplates("mysql").getClass());
  }

  @Test
  public void mariadb() {
    assertEquals(MySQLTemplates.class, QueryDSL.toSQLTemplates("mariadb").getClass());
  }

  @Test
  public void h2() {
    assertEquals(H2Templates.class, QueryDSL.toSQLTemplates("h2").getClass());
  }

  @Test
  public void hsqldb() {
    assertEquals(HSQLDBTemplates.class, QueryDSL.toSQLTemplates("hsqldb").getClass());
  }

  @Test
  public void pgsql() {
    assertEquals(PostgreSQLTemplates.class, QueryDSL.toSQLTemplates("pgsql").getClass());
  }

  @Test
  public void postgresql() {
    assertEquals(PostgreSQLTemplates.class, QueryDSL.toSQLTemplates("postgresql").getClass());
  }

  @Test
  public void sqlite() {
    assertEquals(SQLiteTemplates.class, QueryDSL.toSQLTemplates("sqlite").getClass());
  }

  @Test
  public void oracle() {
    assertEquals(OracleTemplates.class, QueryDSL.toSQLTemplates("oracle").getClass());
  }

  @Test
  public void firebirdsql() {
    assertEquals(FirebirdTemplates.class, QueryDSL.toSQLTemplates("firebirdsql").getClass());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailOnError() {
    QueryDSL.toSQLTemplates("xxx");
  }

  private Block sqlqueryfactory() {
    return unit -> {
      unit.constructor(DataSource.class);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block sqlqueryfactory(final String name) {
    return unit -> {
      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(unit.get(SQLQueryFactory.class));

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(key(SQLQueryFactory.class, name))).andReturn(lbb);
    };
  }

  private <T extends SQLTemplates> Block conf(final Class<T> t) {
    return unit -> {
      Configuration conf = unit.constructor(Configuration.class)
          .args(SQLTemplates.class)
          .build(isA(t));

      unit.registerMock(Configuration.class, conf);
    };
  }

  @SuppressWarnings("unchecked")
  private Block bindconf(final String name) {
    return unit -> {
      LinkedBindingBuilder<Configuration> lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(unit.get(Configuration.class));

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(key(Configuration.class, name))).andReturn(lbb);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private <T extends SQLTemplates> Block sqlTemplate(final Class<T> template, final String name) {
    return unit -> {
      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(isA(template));

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(key(SQLTemplates.class, name))).andReturn(lbb);
    };
  }

  private <T> Key<T> key(final Class<T> template, final String name) {
    return name == null ? Key.get(template) : Key.get(template, Names.named(name));
  }

  public Config config(final String db) {
    return new QueryDSL().config()
        .withValue("db", ConfigValueFactory.fromAnyRef(db))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("jdbctest"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .resolve();
  }

  @SuppressWarnings("unchecked")
  private Block serviceKey(final String db) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(new Env.ServiceKey()).times(2);

      AnnotatedBindingBuilder<DataSource> binding = unit.mock(AnnotatedBindingBuilder.class);
      binding.toInstance(unit.get(HikariDataSource.class));
      binding.toInstance(unit.get(HikariDataSource.class));

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
      expect(binder.bind(Key.get(DataSource.class, Names.named(db)))).andReturn(binding);
    };
  }

  private Block hikariConfig() {
    return unit -> {
      Properties properties = unit.get(Properties.class);
      HikariConfig hikari = unit.constructor(HikariConfig.class)
          .build(properties);
      unit.registerMock(HikariConfig.class, hikari);
    };
  }

  private Block hikariDataSource(final String url) {
    return unit -> {
      HikariConfig properties = unit.get(HikariConfig.class);
      HikariDataSource hikari = unit.constructor(HikariDataSource.class)
          .build(properties);

      unit.registerMock(HikariDataSource.class, hikari);
    };
  }

  private Block props(final String dataSourceClassName, final String url, final String name,
      final String username, final String password, final boolean hasDataSourceClassName) {
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
      expect(properties
          .setProperty("dataSource.url", url))
              .andReturn(null);

      expect(properties.containsKey("dataSourceClassName")).andReturn(hasDataSourceClassName);
      if (!hasDataSourceClassName) {
        expect(properties.getProperty("dataSource.dataSourceClassName"))
            .andReturn(dataSourceClassName);
        expect(properties.setProperty("dataSourceClassName", dataSourceClassName)).andReturn(null);
      }
      expect(properties.remove("dataSource.dataSourceClassName")).andReturn(dataSourceClassName);
      expect(properties.setProperty("poolName", name)).andReturn(null);

      unit.registerMock(Properties.class, properties);
    };
  }
}
