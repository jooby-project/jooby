package org.jooby.hbm;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.Router;
import org.jooby.internal.hbm.HbmUnitDescriptor;
import org.jooby.jdbc.Jdbc;
import org.jooby.scope.Providers;
import org.jooby.scope.RequestScoped;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbm.class, HbmUnitDescriptor.class, Multibinder.class,
    Route.Definition.class, Properties.class, Bootstrap.class, Providers.class })
public class HbmTest {

  Config config = ConfigFactory.parseResources(Hbm.class, "hbm.conf")
      .withFallback(ConfigFactory.parseResources(Jdbc.class, "jdbc.conf"))
      .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
      .withValue("application.ns", ConfigValueFactory.fromAnyRef("x.y.z"));

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();

    new MockUnit(Env.class, Binder.class, HibernateEntityManagerFactory.class)
        .expect(env("dev"))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("jdbctest"))
        .expect(onStop)
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void prod() throws Exception {
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();

    new MockUnit(Env.class, Binder.class, HibernateEntityManagerFactory.class)
        .expect(env("prod"))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("jdbctest"))
        .expect(onStop)
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void onStop() throws Exception {
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();

    new MockUnit(Env.class, Binder.class, HibernateEntityManagerFactory.class)
        .expect(env("dev"))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("jdbctest"))
        .expect(onStop)
        .expect(unit -> {
          HibernateEntityManagerFactory em = unit.get(HibernateEntityManagerFactory.class);
          em.close();
        })
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        }, unit -> {
          List<CheckedRunnable> captured = unit.captured(CheckedRunnable.class);
          captured.get(0).run();
        });
  }

  @Test
  public void defaultsScan() throws Exception {
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();

    new MockUnit(Env.class, Binder.class, HibernateEntityManagerFactory.class)
        .expect(env("dev"))
        .expect(props("org.h2.jdbcx.JdbcDataSource", "jdbc:h2:target/jdbctest", "h2.jdbctest",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource())
        .expect(serviceKey("jdbctest"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf, "x.y.z"))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("jdbctest"))
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .scan()
              .configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void config() {
    assertEquals(ConfigFactory.parseResources(Hbm.class, "hbm.conf")
        .withFallback(ConfigFactory.parseResources(Jdbc.class, "jdbc.conf")),
        new Hbm().config());
  }

  private Block openSessionInView(final String name) {
    return unit -> {
      OpenSessionInView osiv = unit.constructor(OpenSessionInView.class)
          .build(unit.get(HibernateEntityManagerFactory.class),
              Arrays.asList(Key.get(EntityManager.class),
                  Key.get(EntityManager.class, Names.named(name))));

      Definition route = unit.mock(Route.Definition.class);
      expect(route.name("hbm")).andReturn(route);
      Router routes = unit.mock(Router.class);
      expect(routes.use("*", "*", osiv)).andReturn(route);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(routes);
    };
  }

  private Block entityManagerFactoryBuilder() {
    return unit -> {
      EntityManagerFactoryBuilder builder = unit.mock(EntityManagerFactoryBuilder.class);
      expect(builder.build()).andReturn(unit.get(HibernateEntityManagerFactory.class));

      unit.mockStatic(Bootstrap.class);
      expect(Bootstrap.getEntityManagerFactoryBuilder(eq(unit.get(HbmUnitDescriptor.class)),
          isA(Map.class))).andReturn(builder);
    };
  }

  private Block descriptor(final ClassLoader classLoader, final Config dbconf,
      final String... packages) {
    return unit -> {
      HbmUnitDescriptor descriptor = unit.constructor(HbmUnitDescriptor.class)
          .build(classLoader, unit.get(HikariDataSource.class), dbconf, Sets.newHashSet(packages));

      unit.registerMock(HbmUnitDescriptor.class, descriptor);
    };
  }

  private Block env(final String name) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.name()).andReturn(name);
    };
  }

  @SuppressWarnings("unchecked")
  private Block serviceKey(final String db) {
    return unit -> {
      ServiceKey skey = new Env.ServiceKey();
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(skey).times(2);

      AnnotatedBindingBuilder<DataSource> binding = unit.mock(AnnotatedBindingBuilder.class);
      binding.toInstance(unit.get(HikariDataSource.class));
      binding.toInstance(unit.get(HikariDataSource.class));

      LinkedBindingBuilder<EntityManagerFactory> emf = unit.mock(LinkedBindingBuilder.class);
      emf.toInstance(unit.get(HibernateEntityManagerFactory.class));
      emf.toInstance(unit.get(HibernateEntityManagerFactory.class));

      Provider<EntityManager> emprovider = unit.mock(Provider.class);

      unit.mockStatic(Providers.class);
      expect(Providers.outOfScope(Key.get(EntityManager.class))).andReturn(emprovider);
      expect(Providers.outOfScope(Key.get(EntityManager.class, Names.named(db))))
          .andReturn(emprovider);

      LinkedBindingBuilder<EntityManager> em = unit.mock(LinkedBindingBuilder.class);
      expect(em.toProvider(emprovider)).andReturn(em);
      expect(em.toProvider(emprovider)).andReturn(em);
      em.in(RequestScoped.class);
      em.in(RequestScoped.class);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
      expect(binder.bind(Key.get(DataSource.class, Names.named(db)))).andReturn(binding);

      expect(binder.bind(Key.get(EntityManagerFactory.class))).andReturn(emf);
      expect(binder.bind(Key.get(EntityManagerFactory.class, Names.named(db)))).andReturn(emf);

      expect(binder.bind(Key.get(EntityManager.class))).andReturn(em);
      expect(binder.bind(Key.get(EntityManager.class, Names.named(db)))).andReturn(em);
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
