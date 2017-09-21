package org.jooby.jooq;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
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
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import javax.sql.DataSource;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({jOOQ.class, DefaultConfiguration.class, DSL.class,
    DataSourceConnectionProvider.class, DefaultTransactionProvider.class})
public class jOOQTest {

  @SuppressWarnings("unchecked")
  private MockUnit.Block configuration = unit -> {
    DataSourceConnectionProvider dscp = unit.constructor(DataSourceConnectionProvider.class)
        .build(unit.get(HikariDataSource.class));

    DefaultTransactionProvider trx = unit.constructor(DefaultTransactionProvider.class)
        .args(ConnectionProvider.class)
        .build(dscp);

    DefaultConfiguration conf = unit.constructor(DefaultConfiguration.class)
        .build();
    expect(conf.set(dscp)).andReturn(conf);
    expect(conf.set(trx)).andReturn(conf);
    expect(conf.set(SQLDialect.H2)).andReturn(conf);

    unit.registerMock(Configuration.class, conf);

    AnnotatedBindingBuilder<Configuration> abbC = unit.mock(AnnotatedBindingBuilder.class);
    abbC.toInstance(conf);
    abbC.toInstance(conf);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(Configuration.class))).andReturn(abbC);
    expect(binder.bind(Key.get(Configuration.class, Names.named("jdbctest")))).andReturn(abbC);
  };

  @SuppressWarnings("unchecked")
  private MockUnit.Block ctx = unit -> {
    DSLContext dslcontext = unit.mock(DSLContext.class);
    Configuration conf = unit.get(Configuration.class);
    unit.mockStatic(DSL.class);
    expect(DSL.using(conf)).andReturn(dslcontext);

    AnnotatedBindingBuilder<DSLContext> abbC = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbC.toProvider(isA(Provider.class))).andReturn(abbC);
    expect(abbC.toProvider(unit.capture(Provider.class))).andReturn(abbC);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(DSLContext.class))).andReturn(abbC);
    expect(binder.bind(Key.get(DSLContext.class, Names.named("jdbctest")))).andReturn(abbC);
  };

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
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
        .expect(configuration)
        .expect(ctx)
        .expect(onStop)
        .run(unit -> {
          new jOOQ()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Provider.class).iterator().next().get();
        });
  }

  @Test
  public void withDbProp() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("jdbctest"))
        .expect(configuration)
        .expect(ctx)
        .expect(onStop)
        .run(unit -> {
          new jOOQ("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Provider.class).iterator().next().get();
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
        .expect(configuration)
        .expect(ctx)
        .expect(onStop)
        .run(unit -> {
          new jOOQ()
              .doWith((final Configuration c) -> assertEquals(unit.get(Configuration.class), c))
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Provider.class).iterator().next().get();
        });
  }

  private Config config() {
    return new jOOQ().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("jdbctest"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
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
      Properties props = unit.mock(Properties.class);
      expect(props.getProperty("url")).andReturn(url);
      expect(hikari.getDataSourceProperties()).andReturn(props);

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
      expect(properties.setProperty("maximumPoolSize", "4")).andReturn(null);

      unit.registerMock(Properties.class, properties);
    };
  }
}
