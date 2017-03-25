package org.jooby.rx;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Properties;

import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.davidmoten.rx.jdbc.Database;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RxJdbc.class, Database.class })
public class RxJdbcTest {

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  private Block bind = unit -> {
    unit.mockStatic(Database.class);

    Database db = unit.mock(Database.class);
    unit.registerMock(Database.class, db);
    expect(Database.fromDataSource(unit.get(HikariDataSource.class))).andReturn(db);

    LinkedBindingBuilder<Database> lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.toInstance(db);
    lbb.toInstance(db);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(Database.class))).andReturn(lbb);
    expect(binder.bind(Key.get(Database.class, Names.named("jdbctest")))).andReturn(lbb);
  };

  @Test
  public void configure() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(bind)
        .expect(onStop)
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void withDb() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(bind)
        .expect(onStop)
        .run(unit -> {
          new RxJdbc("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void onStop() throws Exception {
    String url = "jdbc:h2:target/jdbctest";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(bind)
        .expect(onStop)
        .expect(unit -> {
          expect(unit.get(Database.class).close()).andReturn(null);
        })
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  private Config config() {
    return new RxJdbc().config()
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

  private Block hikariDataSource() {
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
