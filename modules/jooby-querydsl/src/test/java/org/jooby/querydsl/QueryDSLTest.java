package org.jooby.querydsl;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.querydsl.sql.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueryDSL.class, Configuration.class, H2Templates.class})
public class QueryDSLTest {

  private Block closeconf = unit -> {
    Configuration conf = unit.get(Configuration.class);
    conf.addListener(SQLCloseListener.DEFAULT);
  };

  private MockUnit.Block managed = unit -> {
    Env env = unit.get(Env.class);
  };

  private Block newSQLQueryFactory = unit -> {
    SQLQueryFactory factory = unit.constructor(SQLQueryFactory.class)
        .build(unit.get(Configuration.class), unit.get(DataSource.class));
    unit.registerMock(SQLQueryFactory.class, factory);
  };
  private Block ds = unit -> {
    Env env = unit.get(Env.class);
    expect(env.serviceKey()).andReturn(new Env.ServiceKey());
    DataSource ds = unit.registerMock(DataSource.class);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));
    expect(env.get(Key.get(String.class, Names.named("db.dbtype")))).andReturn(Optional.of("h2"));
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(ds)
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "db"))
        .expect(conf(H2Templates.class))
        .expect(bindconf(null))
        .expect(bindconf("db"))
        .expect(newSQLQueryFactory)
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("db"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void dbProp() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(ds)
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "db"))
        .expect(conf(H2Templates.class))
        .expect(bindconf(null))
        .expect(bindconf("db"))
        .expect(newSQLQueryFactory)
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("db"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL("db")
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void with() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(ds)
        .expect(sqlTemplate(CUBRIDTemplates.class, null))
        .expect(sqlTemplate(CUBRIDTemplates.class, "db"))
        .expect(conf(CUBRIDTemplates.class))
        .expect(newSQLQueryFactory)
        .expect(bindconf(null))
        .expect(bindconf("db"))
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("db"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .with(new CUBRIDTemplates())
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(ds)
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "db"))
        .expect(conf(H2Templates.class))
        .expect(newSQLQueryFactory)
        .expect(closeconf)
        .expect(bindconf(null))
        .expect(bindconf("db"))
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("db"))
        .expect(managed)
        .run(unit -> {
          new QueryDSL()
              .doWith((final Configuration conf) -> {
                conf.addListener(SQLCloseListener.DEFAULT);
              })
              .configure(unit.get(Env.class), config("fs"), unit.get(Binder.class));
        });
  }

  @Test
  public void newSQLFactory() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(ds)
        .expect(sqlTemplate(H2Templates.class, null))
        .expect(sqlTemplate(H2Templates.class, "db"))
        .expect(conf(H2Templates.class))
        .expect(newSQLQueryFactory)
        .expect(bindconf(null))
        .expect(bindconf("db"))
        .expect(sqlqueryfactory())
        .expect(sqlqueryfactory(null))
        .expect(sqlqueryfactory("db"))
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

  @Test
  public void sqlserver() {
    assertEquals(SQLServer2012Templates.class, QueryDSL.toSQLTemplates("sqlserver").getClass());
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

  @SuppressWarnings({"unchecked", "rawtypes"})
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

  @SuppressWarnings({"unchecked", "rawtypes"})
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
        .resolve();
  }
}
