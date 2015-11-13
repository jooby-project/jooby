package org.jooby.ebean;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Properties;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.internal.ebean.EbeanEnhancer;
import org.jooby.internal.ebean.EbeanManaged;
import org.jooby.internal.ebean.ForwardingDataSource;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ContainerConfig;
import com.avaje.ebean.config.ServerConfig;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ebeanby.class, ServerConfig.class, EbeanEnhancer.class })
public class EbeanbyTest {

  @SuppressWarnings("unchecked")
  private Block jdbc = unit -> {
    Binder binder = unit.get(Binder.class);

    ScopedBindingBuilder scope = unit.mock(ScopedBindingBuilder.class);
    scope.asEagerSingleton();
    scope.asEagerSingleton();

    LinkedBindingBuilder<DataSource> binding = unit.mock(LinkedBindingBuilder.class);
    expect(binding.toProvider(isA(Provider.class))).andReturn(scope).times(2);
    expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
    expect(binder.bind(Key.get(DataSource.class, Names.named("db")))).andReturn(binding);
  };

  private Block containerConfig = unit -> {
    ContainerConfig config = unit.mockConstructor(ContainerConfig.class);

    config.loadFromProperties(isA(Properties.class));

    unit.registerMock(ContainerConfig.class, config);
  };

  @SuppressWarnings("unchecked")
  private Block binder = unit -> {
    Binder binder = unit.get(Binder.class);

    ScopedBindingBuilder sbbES = unit.mock(ScopedBindingBuilder.class);
    sbbES.asEagerSingleton();
    sbbES.asEagerSingleton();

    LinkedBindingBuilder<EbeanServer> lbbES = unit.mock(LinkedBindingBuilder.class);
    expect(lbbES.toProvider(isA(EbeanManaged.class))).andReturn(sbbES).times(2);

    expect(binder.bind(Key.get(EbeanServer.class))).andReturn(lbbES);
    expect(binder.bind(Key.get(EbeanServer.class, Names.named("db")))).andReturn(lbbES);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(jdbc)
        .expect(containerConfig)
        .expect(serverConfig(true))
        .expect(enhancer("my.model"))
        .expect(binder)
        .run(unit -> {
          new Ebeanby("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configureWithPackages() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(jdbc)
        .expect(containerConfig)
        .expect(serverConfig(true))
        .expect(enhancer("otro.package", "my.model"))
        .expect(binder)
        .expect(unit -> {
          ServerConfig conf = unit.get(ServerConfig.class);
          conf.addPackage("otro.package");
        })
        .run(unit -> {
          new Ebeanby()
              .packages("otro.package")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configureCallback() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(jdbc)
        .expect(containerConfig)
        .expect(serverConfig(true))
        .expect(enhancer("my.model"))
        .expect(binder)
        .expect(unit -> {
          ServerConfig conf = unit.get(ServerConfig.class);
          conf.setName("xx");
        })
        .run(unit -> {
          new Ebeanby()
              .doWith(conf -> {
            conf.setName("xx");
          })
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void configureCustomOption() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(jdbc)
        .expect(containerConfig)
        .expect(serverConfig(false))
        .expect(enhancer("my.model"))
        .expect(binder)
        .run(unit -> {
          Config customConfig = config().withValue("ebean.db.defaultServer",
              ConfigValueFactory.fromAnyRef(false));

          new Ebeanby("db")
              .configure(unit.get(Env.class), customConfig, unit.get(Binder.class));
        });
  }

  private Config config() {
    return new Ebeanby().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("model"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
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

  private Block serverConfig(final boolean defaultServer) {
    return unit -> {
      ServerConfig serverConfig = unit.mockConstructor(ServerConfig.class);

      serverConfig.setName("db");
      serverConfig.addPackage("my.model");
      serverConfig.setContainerConfig(unit.get(ContainerConfig.class));
      serverConfig.setDataSource(isA(ForwardingDataSource.class));
      serverConfig.loadFromProperties(isA(Properties.class));
      serverConfig.setDefaultServer(defaultServer);
      serverConfig.setRegister(true);

      unit.registerMock(ServerConfig.class, serverConfig);
    };
  }
}
