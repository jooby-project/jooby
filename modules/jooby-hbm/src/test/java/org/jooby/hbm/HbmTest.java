package org.jooby.hbm;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hbm5.Beer;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Registry;
import org.jooby.funzy.Throwing;
import org.jooby.internal.hbm.*;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbm.class, BootstrapServiceRegistryBuilder.class,
    MetadataSources.class, CompletableFuture.class, GuiceBeanManager.class, SessionProvider.class,
    OpenSessionInView.class})
public class HbmTest {

  private Block bsrb = unit -> {
    BootstrapServiceRegistryBuilder bsrb = unit.constructor(BootstrapServiceRegistryBuilder.class)
        .build();
    unit.registerMock(BootstrapServiceRegistryBuilder.class, bsrb);

    BootstrapServiceRegistry bsr = unit.mock(BootstrapServiceRegistry.class);
    unit.registerMock(BootstrapServiceRegistry.class, bsr);

    expect(bsrb.build()).andReturn(bsr);
  };

  private Block applyDataSource = unit -> {
    DataSource ds = unit.registerMock(DataSource.class);
    Env env = unit.get(Env.class);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

    StandardServiceRegistryBuilder ssrb = unit.get(StandardServiceRegistryBuilder.class);
    expect(ssrb.applySetting(AvailableSettings.DATASOURCE, unit.get(DataSource.class)))
        .andReturn(ssrb);
  };

  @SuppressWarnings("unchecked")
  private Block onStart = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);
  };

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  @Test
  public void shouldLoadConfigFromClasspath() {
    assertEquals(ConfigFactory.parseResources(Hbm.class, "hbm.conf"), new Hbm().config());
  }

  @Test
  public void newHbm() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void newHbmNoDataSource() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.empty());
        })
        .run(unit -> {
          new Hbm()
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void onStart() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm("db")
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        }, unit -> {
          Throwing.Consumer<Registry> onStart = unit.captured(Throwing.Consumer.class).iterator()
              .next();
          onStart.accept(unit.get(Registry.class));

          CompletableFuture promise = unit.captured(CompletableFuture.class).iterator().next();
          assertEquals(unit.get(Registry.class), promise.get());
        });
  }

  @Test
  public void onStop() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          unit.get(SessionFactory.class).close();
        })
        .run(unit -> {
          new Hbm()
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        }, unit -> {
          Throwing.Runnable onStop = unit.captured(Throwing.Runnable.class).iterator()
              .next();
          onStop.run();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void withEvent() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          PostLoadEventListener listener = unit.mock(PostLoadEventListener.class);
          Registry registry = unit.get(Registry.class);
          expect(registry.require(PostLoadEventListener.class))
              .andReturn(listener);

          EventListenerRegistry elr = unit.mock(EventListenerRegistry.class);
          elr.appendListeners(EventType.POST_LOAD, listener);

          ServiceRegistryImplementor sri = unit.mock(ServiceRegistryImplementor.class);
          expect(sri.getService(EventListenerRegistry.class)).andReturn(elr);

          SessionFactoryImplementor sf = unit.get(SessionFactoryImplementor.class);
          expect(sf.getServiceRegistry()).andReturn(sri);
        })
        .expect(unit -> {
          AnnotatedBindingBuilder abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(PostLoadEventListener.class)).andReturn(abb);
        })
        .run(unit -> {
          new Hbm()
              .onEvent(EventType.POST_LOAD, PostLoadEventListener.class)
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        }, unit -> {
          Throwing.Consumer<Registry> onStart = unit.captured(Throwing.Consumer.class).iterator()
              .next();
          onStart.accept(unit.get(Registry.class));
        });
  }

  @Test
  public void addClass() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources(Beer.class))
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .classes(Beer.class)
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultScan() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources("my.model"))
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .scan()
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void scanViaProperty() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources("foo.bar"))
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .scan()
              .configure(unit.get(Env.class), config("hbm")
                      .withValue("hibernate.packagesToScan",
                          ConfigValueFactory.fromAnyRef(Arrays.asList("foo.bar"))),
                  unit.get(Binder.class));
        });
  }

  @Test
  public void scan() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources("my.model"))
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .scan("my.model")
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void shouldPickupCustomHibernateProps() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.DIALECT, "h2",
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .configure(unit.get(Env.class),
                  config("hbm").withValue("hibernate.dialect", ConfigValueFactory.fromAnyRef("h2")),
                  unit.get(Binder.class));
        });
  }

  @Test
  public void ddlAutoIsNoneOnProd() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void genericSetupCallbackShouldWork() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          BootstrapServiceRegistryBuilder bsrb = unit.get(BootstrapServiceRegistryBuilder.class);
          expect(bsrb.applyIntegrator(unit.get(Integrator.class))).andReturn(bsrb);
        })
        .run(unit -> {
          new Hbm()
              .doWithBootstrap((final BootstrapServiceRegistryBuilder bsrb) -> {
                bsrb.applyIntegrator(unit.get(Integrator.class));
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithRegistry() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          StandardServiceRegistryBuilder builder = unit.get(StandardServiceRegistryBuilder.class);
          expect(builder.configure("resourceName")).andReturn(builder);
        })
        .run(unit -> {
          new Hbm()
              .doWithRegistry((final StandardServiceRegistryBuilder builder) -> {
                builder.configure("resourceName");
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithSources() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          MetadataSources builder = unit.get(MetadataSources.class);
          expect(builder.addPackage("foo.bar")).andReturn(builder);
        })
        .run(unit -> {
          new Hbm()
              .doWithSources((final MetadataSources builder) -> {
                builder.addPackage("foo.bar");
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithSessionFactoryBuilder() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
          SessionFactoryBuilder builder = unit.get(SessionFactoryBuilder.class);
          expect(builder.applyAutoClosing(true)).andReturn(builder);
        })
        .run(unit -> {
          new Hbm()
              .doWithSessionFactoryBuilder((final SessionFactoryBuilder builder) -> {
                builder.applyAutoClosing(true);
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithSessionBuilder() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class, SessionBuilderImplementor.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
                AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
                AvailableSettings.SCANNER_DISCOVERY, "class",
                AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit -> {
            SessionBuilder builder = unit.get(SessionBuilderImplementor.class);
            expect(unit.get(SessionFactory.class).withOptions()).andReturn(builder);
            expect(builder.setQueryParameterValidation(true)).andReturn(builder);
            expect(builder.openSession()).andReturn(null);
        })
        .run(unit -> {
            new Hbm()
                    .doWithSessionBuilder((final SessionBuilder builder) -> {
                        builder.setQueryParameterValidation(true);
                    })
                    .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));

            // invoke the captured object passed to the SessionProvider / UnitOfWorkProvider
            unit.captured(SessionBuilderConfigurer.class).get(0).apply(unit.get(SessionFactory.class));
        });
  }

  @Test(expected = ClassCastException.class)
  public void genericSetupCallbackShouldReportClassCastException() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(props("org.h2.jdbcx.JdbcDataSource", url, "h2.hbm",
            "sa", "", false))
        .expect(hikariConfig())
        .expect(hikariDataSource(url))
        .expect(serviceKey("hbm"))
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("hbm"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("hbm", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("hbm", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("hbm", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("hbm", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("hbm", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .doWithBootstrap((final BootstrapServiceRegistryBuilder bsrb) -> {
                Object value = "";
                System.out.println(((Number) value).intValue());
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test(expected = NullPointerException.class)
  public void genericSetupCallbackShouldReportException() throws Exception {
    String url = "jdbc:h2:target/hbm";
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm()
              .doWithSessionFactory((final SessionFactory bsrb) -> {
                throw new NullPointerException();
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void doWithSessionFactory() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Integrator.class)
        .expect(env("prod"))
        .expect(bsrb)
        .expect(ssrb("none"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .expect(unit-> {
          SessionFactory sessionFactory = unit.get(SessionFactory.class);
          expect(sessionFactory.isClosed()).andReturn(true);
        })
        .run(unit -> {
          new Hbm()
              .doWithSessionFactory((final SessionFactory factory) -> {
                factory.isClosed();
              })
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  @Test
  public void newHbmWithDbProp() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(env("dev"))
        .expect(bsrb)
        .expect(ssrb("update"))
        .expect(applySettins(ImmutableMap.of(
            AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false,
            AvailableSettings.SCANNER_DISCOVERY, "class",
            AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed")))
        .expect(applyDataSource)
        .expect(metadataSources())
        .expect(metadataBuilder())
        .expect(sessionFactoryBuilder("db"))
        .expect(beanManager())
        .expect(bind(null, SessionFactory.class))
        .expect(bind("db", SessionFactory.class))
        .expect(bind(null, EntityManagerFactory.class))
        .expect(bind("db", EntityManagerFactory.class))
        .expect(sessionProvider())
        .expect(bind(null, Session.class, SessionProvider.class))
        .expect(bind("db", Session.class, SessionProvider.class))
        .expect(bind(null, EntityManager.class, SessionProvider.class))
        .expect(bind("db", EntityManager.class, SessionProvider.class))
        .expect(unitOfWork())
        .expect(bind(null, UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(bind("db", UnitOfWork.class, UnitOfWorkProvider.class))
        .expect(onStart)
        .expect(onStop)
        .run(unit -> {
          new Hbm("db")
              .configure(unit.get(Env.class), config("hbm"), unit.get(Binder.class));
        });
  }

  private Block sessionProvider() {
    return unit -> {
      SessionFactory sf = unit.get(SessionFactory.class);

      SessionProvider sp = unit.constructor(SessionProvider.class)
          .args(SessionFactory.class, SessionBuilderConfigurer.class)
          .build(eq(sf), unit.capture(SessionBuilderConfigurer.class));

      unit.registerMock(SessionProvider.class, sp);
    };
  }

  private Block unitOfWork() {
    return unit -> {
      SessionFactory sf = unit.get(SessionFactory.class);

      UnitOfWorkProvider sp = unit.constructor(UnitOfWorkProvider.class)
          .args(SessionFactory.class, SessionBuilderConfigurer.class)
          .build(eq(sf), unit.capture(SessionBuilderConfigurer.class));

      unit.registerMock(UnitOfWorkProvider.class, sp);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Block bind(final String name, final Class type) {
    return unit -> {
      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(unit.get(type));
      Binder binder = unit.get(Binder.class);
      Key key = name == null ? Key.get(type) : Key.get(type, Names.named(name));
      expect(binder.bind(key)).andReturn(lbb);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Block bind(final String name, final Class type,
      final Class<? extends Provider> provider) {
    return unit -> {
      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      expect(lbb.toProvider(unit.get(provider))).andReturn(lbb);

      Binder binder = unit.get(Binder.class);
      Key key = name == null ? Key.get(type) : Key.get(type, Names.named(name));
      expect(binder.bind(key)).andReturn(lbb);
    };
  }

  @SuppressWarnings("unchecked")
  private Block beanManager() {
    return unit -> {
      unit.mockStatic(GuiceBeanManager.class);

      StandardServiceRegistryBuilder ssrb = unit.get(StandardServiceRegistryBuilder.class);

      expect(ssrb.applySetting(org.hibernate.cfg.AvailableSettings.DELAY_CDI_ACCESS, true))
                .andReturn(ssrb);

      BeanManager bm = unit.mock(BeanManager.class);
      unit.registerMock(BeanManager.class, bm);

      expect(GuiceBeanManager.beanManager(unit.capture(CompletableFuture.class))).andReturn(bm);

      expect(ssrb.applySetting(org.hibernate.cfg.AvailableSettings.CDI_BEAN_MANAGER, bm))
                .andReturn(ssrb);
    };
  }

  private Block sessionFactoryBuilder(final String name) {
    return unit -> {
      SessionFactoryBuilder sfb = unit.get(SessionFactoryBuilder.class);
      expect(sfb.applyName(name)).andReturn(sfb);

      SessionFactoryImplementor sf = unit.mock(SessionFactoryImplementor.class);
      expect(sfb.build()).andReturn(sf);
      unit.registerMock(SessionFactory.class, sf);
      unit.registerMock(SessionFactoryImplementor.class, sf);
      unit.registerMock(EntityManagerFactory.class, sf);
    };
  }

  private Block metadataBuilder() {
    return unit -> {
      MetadataBuilder mb = unit.get(MetadataBuilder.class);
      expect(mb.applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE))
          .andReturn(mb);
      expect(mb.applyScanEnvironment(unit.get(ScanEnvironment.class))).andReturn(mb);

      SessionFactoryBuilder sfb = unit.mock(SessionFactoryBuilder.class);
      unit.registerMock(SessionFactoryBuilder.class, sfb);

      Metadata md = unit.mock(Metadata.class);
      expect(md.getSessionFactoryBuilder()).andReturn(sfb);

      expect(mb.build()).andReturn(md);

      unit.registerMock(Metadata.class, md);
    };
  }

  @SuppressWarnings("rawtypes")
  private Block metadataSources(final Object... resources) {
    return unit -> {
      StandardServiceRegistry ssr = unit.get(StandardServiceRegistry.class);
      MetadataSources sources = unit.constructor(MetadataSources.class)
          .build(ssr);
      unit.registerMock(MetadataSources.class, sources);

      List<String> packages = Arrays.asList(resources)
          .stream()
          .filter(String.class::isInstance)
          .map(s -> s.toString())
          .collect(Collectors.toList());

      List<Class> classes = Arrays.asList(resources)
          .stream()
          .filter(Class.class::isInstance)
          .map(it -> (Class) it)
          .collect(Collectors.toList());

      MetadataBuilder mb = unit.mock(MetadataBuilder.class);
      unit.registerMock(MetadataBuilder.class, mb);

      expect(sources.getAnnotatedPackages()).andReturn(packages);
      expect(sources.getMetadataBuilder()).andReturn(mb);
      packages.forEach(c -> {
        expect(sources.addPackage(c)).andReturn(sources);
      });
      classes.forEach(c -> {
        expect(sources.addAnnotatedClass(c)).andReturn(sources);
      });

      List<URL> urls = packages
          .stream()
          .map(pkg -> getClass().getResource("/" + pkg.replace('.', '/')))
          .collect(Collectors.toList());

      ScanEnvImpl scanenv = unit.constructor(ScanEnvImpl.class)
          .build(urls);

      unit.registerMock(ScanEnvironment.class, scanenv);
    };
  }

  private Block applySettins(final Map<String, Object> settings) {
    return unit -> {
      StandardServiceRegistryBuilder ssrb = unit.get(StandardServiceRegistryBuilder.class);
      expect(ssrb.applySettings(settings)).andReturn(ssrb);
    };
  }

  private Block ssrb(final String ddl) {
    return unit -> {
      StandardServiceRegistryBuilder ssrb = unit.constructor(StandardServiceRegistryBuilder.class)
          .build(unit.get(BootstrapServiceRegistry.class));

      expect(ssrb.applySetting(AvailableSettings.HBM2DDL_AUTO, ddl)).andReturn(ssrb);

      StandardServiceRegistry ssr = unit.mock(StandardServiceRegistry.class);
      unit.registerMock(StandardServiceRegistry.class, ssr);

      expect(ssrb.build()).andReturn(ssr);

      unit.registerMock(StandardServiceRegistryBuilder.class, ssrb);
    };
  }

  private Block env(final String name) {
    return unit -> {
      Env env = unit.get(Env.class);
      ServiceKey skey = new Env.ServiceKey();
      expect(env.serviceKey()).andReturn(skey);
      expect(env.name()).andReturn(name);
    };
  }

  private Config config(final String db) {
    return new Hbm().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("fs"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef(db))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();
  }

  @SuppressWarnings("unchecked")
  private Block serviceKey(final String db) {
    return unit -> {
      Env env = unit.get(Env.class);
      ServiceKey skey = new Env.ServiceKey();
      expect(env.serviceKey()).andReturn(skey).times(2);

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
