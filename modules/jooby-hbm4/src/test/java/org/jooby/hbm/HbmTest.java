package org.jooby.hbm;

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

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

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
import org.jooby.funzy.Throwing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbm.class, HbmUnitDescriptor.class, Multibinder.class,
    Route.Definition.class, Properties.class, Bootstrap.class, Providers.class})
public class HbmTest {

  Config config = ConfigFactory.parseResources(Hbm.class, "hbm.conf")
      .withFallback(ConfigFactory.parseResources(Jdbc.class, "jdbc.conf"))
      .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
      .withValue("application.ns", ConfigValueFactory.fromAnyRef("x.y.z"));

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
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
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(serviceKey("db"))
        .expect(openSessionInView("db"))
        .expect(onStop)
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void noDataSource() throws Exception {
    Config dbconf = config.withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.name", fromAnyRef("jdbctest"))
        .withValue("application.tmpdir", fromAnyRef("target"))
        .withValue("application.charset", fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();

    new MockUnit(Env.class, Binder.class, HibernateEntityManagerFactory.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.empty());
        })
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
        .expect(serviceKey("db"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("db"))
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
        .expect(serviceKey("db"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("db"))
        .expect(onStop)
        .expect(unit -> {
          HibernateEntityManagerFactory em = unit.get(HibernateEntityManagerFactory.class);
          em.close();
        })
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        }, unit -> {
          List<Throwing.Runnable> captured = unit.captured(Throwing.Runnable.class);
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
        .expect(serviceKey("db"))
        .expect(descriptor(Hbm.class.getClassLoader(), dbconf, "x.y.z"))
        .expect(entityManagerFactoryBuilder())
        .expect(openSessionInView("db"))
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .scan()
              .configure(unit.get(Env.class), dbconf, unit.get(Binder.class));
        });
  }

  @Test
  public void config() {
    assertEquals(ConfigFactory.parseResources(Hbm.class, "hbm.conf"), new Hbm().config());
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
      DataSource ds = unit.registerMock(HikariDataSource.class);
      Env env = unit.get(Env.class);
      expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

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
      expect(env.serviceKey()).andReturn(skey);

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

      expect(binder.bind(Key.get(EntityManagerFactory.class))).andReturn(emf);
      expect(binder.bind(Key.get(EntityManagerFactory.class, Names.named(db)))).andReturn(emf);

      expect(binder.bind(Key.get(EntityManager.class))).andReturn(em);
      expect(binder.bind(Key.get(EntityManager.class, Names.named(db)))).andReturn(em);
    };
  }

}
