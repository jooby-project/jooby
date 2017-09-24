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
import java.util.Optional;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({jOOQ.class, DefaultConfiguration.class, DSL.class,
    DataSourceConnectionProvider.class, DefaultTransactionProvider.class})
public class jOOQTest {

  @SuppressWarnings("unchecked")
  private MockUnit.Block configuration = unit -> {
    Env env = unit.get(Env.class);
    expect(env.serviceKey()).andReturn(new Env.ServiceKey());

    Properties properties = unit.mock(Properties.class);
    expect(properties.getProperty("url")).andReturn("jdbc:h2:");

    HikariDataSource ds = unit.registerMock(HikariDataSource.class);
    expect(ds.getDataSourceProperties()).andReturn(properties);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

    DataSourceConnectionProvider dscp = unit.constructor(DataSourceConnectionProvider.class)
        .build(ds);

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
    expect(binder.bind(Key.get(Configuration.class, Names.named("db")))).andReturn(abbC);
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
    expect(binder.bind(Key.get(DSLContext.class, Names.named("db")))).andReturn(abbC);
  };

  private MockUnit.Block onStop = unit -> {
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
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
    new MockUnit(Env.class, Config.class, Binder.class)
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
    new MockUnit(Env.class, Config.class, Binder.class)
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
        .resolve();
  }

}
