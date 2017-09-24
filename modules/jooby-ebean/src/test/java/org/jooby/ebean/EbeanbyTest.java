package org.jooby.ebean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.EbeanServer;
import io.ebean.config.ContainerConfig;
import io.ebean.config.ServerConfig;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.internal.ebean.EbeanEnhancer;
import org.jooby.internal.ebean.EbeanManaged;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.sql.DataSource;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ebeanby.class, ServerConfig.class, EbeanEnhancer.class, Properties.class})
public class EbeanbyTest {

  private Block containerConfig = unit -> {
    DataSource ds = unit.mock(DataSource.class);
    Env env = unit.get(Env.class);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

    ContainerConfig config = unit.mockConstructor(ContainerConfig.class);

    config.loadFromProperties(isA(Properties.class));

    unit.registerMock(ContainerConfig.class, config);
  };

  @SuppressWarnings("unchecked")
  private Block binder = unit -> {
    ServiceKey skey = new Env.ServiceKey();
    Env env = unit.get(Env.class);
    expect(env.serviceKey()).andReturn(skey);

    Binder binder = unit.get(Binder.class);

    ScopedBindingBuilder sbbES = unit.mock(ScopedBindingBuilder.class);

    LinkedBindingBuilder<EbeanServer> lbbES = unit.mock(LinkedBindingBuilder.class);
    expect(lbbES.toProvider(isA(EbeanManaged.class))).andReturn(sbbES).times(2);

    expect(binder.bind(Key.get(EbeanServer.class))).andReturn(lbbES);
    expect(binder.bind(Key.get(EbeanServer.class, Names.named("db")))).andReturn(lbbES);
  };

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);

    expect(env.onStart(isA(Throwing.Runnable.class))).andReturn(env);
    expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
  };

  private Block onStart = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  @Test(expected = NoSuchElementException.class)
  public void noDataSource() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.empty());

          ContainerConfig config = unit.mockConstructor(ContainerConfig.class);

          config.loadFromProperties(isA(Properties.class));

          unit.registerMock(ContainerConfig.class, config);
        })
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(containerConfig)
        .expect(serverConfig(true, "my.model"))
        .expect(ebeanProperties())
        .expect(binder)
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void runEnhancerFirst() throws Exception {
    new MockUnit()
        .expect(enhancer("foo", "bar"))
        .run(unit -> {
          try {
            Ebeanby.PKG.set(ImmutableSet.of("foo", "bar"));
            Ebeanby.runEnhancer().run();
          } finally {
            Ebeanby.PKG.set(new HashSet<>());
          }
        });
  }

  @Test
  public void runEnhancerTwice() throws Exception {
    new MockUnit()
        .run(unit -> {
          try {
            Ebeanby.PKG.set(null);
            Ebeanby.runEnhancer().run();
          } finally {
            Ebeanby.PKG.set(new HashSet<>());
          }
        });
  }

  @Test
  public void configureNoPackage() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(containerConfig)
        .expect(unit -> {
          ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

          serverConfig.setName("db");
          expect(serverConfig.getPackages()).andReturn(null);
          serverConfig.setPackages(Arrays.asList("my.model"));
          serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
          serverConfig.setDataSource(isA(DataSource.class));
          serverConfig.loadFromProperties(isA(Properties.class));
          serverConfig.setDefaultServer(true);
          serverConfig.setRegister(true);

          unit.registerMock(ServerConfig.class, serverConfig);
        })
        .expect(ebeanProperties())
        .expect(binder)
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configurePackages() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(containerConfig)
        .expect(unit -> {
          ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

          serverConfig.setName("db");
          serverConfig.addPackage("foo");
          serverConfig.addPackage("bar");
          expect(serverConfig.getPackages()).andReturn(Arrays.asList("foo", "bar"));
          serverConfig.setPackages(Arrays.asList("foo", "bar"));
          serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
          serverConfig.setDataSource(isA(DataSource.class));
          serverConfig.loadFromProperties(isA(Properties.class));
          serverConfig.setDefaultServer(true);
          serverConfig.setRegister(true);

          unit.registerMock(ServerConfig.class, serverConfig);
        })
        .expect(ebeanProperties())
        .expect(binder)
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby("db")
              .doWith((final ServerConfig conf) -> {
                conf.addPackage("foo");
                conf.addPackage("bar");
              })
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configureEmptyPackage() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(containerConfig)
        .expect(unit -> {
          ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

          serverConfig.setName("db");
          expect(serverConfig.getPackages()).andReturn(ImmutableList.of());
          serverConfig.setPackages(Arrays.asList("my.model"));
          serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
          serverConfig.setDataSource(isA(DataSource.class));
          serverConfig.loadFromProperties(isA(Properties.class));
          serverConfig.setDefaultServer(true);
          serverConfig.setRegister(true);

          unit.registerMock(ServerConfig.class, serverConfig);
        })
        .expect(ebeanProperties())
        .expect(binder)
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void configureWith2Names() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(containerConfig)
        .expect(unit -> {
          ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

          serverConfig.setName("db");
          expect(serverConfig.getPackages()).andReturn(ImmutableList.of("my.model"));
          serverConfig.setPackages(ImmutableList.of("my.model"));
          serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
          serverConfig.setDataSource(isA(DataSource.class));
          serverConfig.loadFromProperties(isA(Properties.class));
          serverConfig.setDefaultServer(true);
          serverConfig.setRegister(true);

          unit.registerMock(ServerConfig.class, serverConfig);
        })
        .expect(ebeanProperties())
        .expect(unit -> {
          ServiceKey skey = new Env.ServiceKey();
          Env env = unit.get(Env.class);
          expect(env.serviceKey()).andReturn(skey);

          Binder binder = unit.get(Binder.class);

          ScopedBindingBuilder sbbES = unit.mock(ScopedBindingBuilder.class);

          LinkedBindingBuilder<EbeanServer> lbbES = unit.mock(LinkedBindingBuilder.class);
          expect(lbbES.toProvider(isA(EbeanManaged.class))).andReturn(sbbES).times(2);

          expect(binder.bind(Key.get(EbeanServer.class))).andReturn(lbbES);
          expect(binder.bind(Key.get(EbeanServer.class, Names.named("db")))).andReturn(lbbES);
        })
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config()
                      .withValue("db", ConfigValueFactory.fromAnyRef("jdbc:db2://127.0.0.1/mydb")),
                  unit.get(Binder.class));
        });
  }

  @Test
  public void configureCallback() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(ebeanProperties())
        .expect(containerConfig)
        .expect(serverConfig(true, "my.model"))
        .expect(binder)
        .expect(unit -> {
          ServerConfig conf = unit.get(ServerConfig.class);
          conf.setName("xx");
        })
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          new Ebeanby()
              .doWith((final ServerConfig conf) -> {
                conf.setName("xx");
              })
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configureCustomOption() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Properties props = unit.constructor(Properties.class).build();
          expect(props.setProperty("ebean.register", "true")).andReturn(null);
          expect(props.setProperty("ebean.defaultServer", "false")).andReturn(null);
          expect(props.setProperty("ebean.ddl.run", "false")).andReturn(null);
          expect(props.setProperty("ebean.logging.txnCommit", "none")).andReturn(null);
          expect(props.setProperty("ebean.logging.directory", "logs")).andReturn(null);
          expect(props.setProperty("ebean.logging.query", "sql")).andReturn(null);
          expect(props.setProperty("ebean.logging.iud", "sql")).andReturn(null);
          expect(props.setProperty("ebean.logging.sqlquery", "sql")).andReturn(null);
          expect(props.setProperty("ebean.logging.logfilesharing", "all")).andReturn(null);
          expect(props.setProperty("ebean.loggingToJavaLogger", "false")).andReturn(null);
          expect(props.setProperty("ebean.ddl.generate", "false")).andReturn(null);
          expect(props.setProperty("ebean.debug.sql", "true")).andReturn(null);
          expect(props.setProperty("ebean.debug.lazyload", "false")).andReturn(null);
          expect(props.setProperty("ebean.disableClasspathSearch", "true")).andReturn(null);
          unit.registerMock(Properties.class, props);
        })
        .expect(containerConfig)
        .expect(serverConfig(false, "my.model"))
        .expect(binder)
        .expect(onStop)
        .expect(onStart)
        .run(unit -> {
          Config customConfig = config().withValue("ebean.db.defaultServer",
              ConfigValueFactory.fromAnyRef(false));

          new Ebeanby("db")
              .configure(unit.get(Env.class), customConfig, unit.get(Binder.class));
        });
  }

  private Config config() {
    return new Ebeanby().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("jdbc:db2://127.0.0.1/db"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("model"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .withValue("runtime.processors-x2", fromAnyRef("4"))
        .resolve();
  }

  private Block enhancer(final String... packages) {
    return unit -> {
      EbeanEnhancer enhancer = unit.mock(EbeanEnhancer.class);
      enhancer.run(Sets.newHashSet(packages));

      unit.mockStatic(EbeanEnhancer.class);
      expect(EbeanEnhancer.newEnhancer()).andReturn(enhancer);
    };
  }

  private Block serverConfig(final boolean defaultServer, final String... packages) {
    return unit -> {
      ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

      serverConfig.setName("db");
      expect(serverConfig.getPackages()).andReturn(Arrays.asList(packages));
      serverConfig.setPackages(Arrays.asList(packages));
      serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
      serverConfig.setDataSource(isA(DataSource.class));
      serverConfig.loadFromProperties(isA(Properties.class));
      serverConfig.setDefaultServer(defaultServer);
      serverConfig.setRegister(true);

      unit.registerMock(ServerConfig.class, serverConfig);
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

  private Block ebeanProperties() {
    return unit -> {
      Properties props = unit.constructor(Properties.class).build();
      expect(props.setProperty("ebean.register", "true")).andReturn(null);
      expect(props.setProperty("ebean.defaultServer", "true")).andReturn(null);
      expect(props.setProperty("ebean.ddl.run", "false")).andReturn(null);
      expect(props.setProperty("ebean.logging.txnCommit", "none")).andReturn(null);
      expect(props.setProperty("ebean.logging.directory", "logs")).andReturn(null);
      expect(props.setProperty("ebean.logging.query", "sql")).andReturn(null);
      expect(props.setProperty("ebean.logging.iud", "sql")).andReturn(null);
      expect(props.setProperty("ebean.logging.sqlquery", "sql")).andReturn(null);
      expect(props.setProperty("ebean.logging.logfilesharing", "all")).andReturn(null);
      expect(props.setProperty("ebean.loggingToJavaLogger", "false")).andReturn(null);
      expect(props.setProperty("ebean.ddl.generate", "false")).andReturn(null);
      expect(props.setProperty("ebean.debug.sql", "true")).andReturn(null);
      expect(props.setProperty("ebean.debug.lazyload", "false")).andReturn(null);
      expect(props.setProperty("ebean.disableClasspathSearch", "true")).andReturn(null);
      unit.registerMock(Properties.class, props);
    };
  }
}
