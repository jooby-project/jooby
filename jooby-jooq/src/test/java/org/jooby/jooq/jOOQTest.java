package org.jooby.jooq;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({jOOQ.class, DefaultConfiguration.class, DSLCtxProviderTest.class,
    DSConnectionProvider.class, DefaultTransactionProvider.class })
public class jOOQTest {

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

  @SuppressWarnings("unchecked")
  private MockUnit.Block configuration = unit -> {
    DSConnectionProvider dscp = unit.constructor(DSConnectionProvider.class)
        .args(Provider.class)
        .build(isA(Provider.class));

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
    Configuration conf = unit.get(Configuration.class);
    DSLCtxProvider ctx = unit.constructor(DSLCtxProvider.class)
        .args(Configuration.class)
        .build(conf);

    AnnotatedBindingBuilder<DSLContext> abbC = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbC.toProvider(ctx)).andReturn(null).times(2);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(DSLContext.class))).andReturn(abbC);
    expect(binder.bind(Key.get(DSLContext.class, Names.named("db")))).andReturn(abbC);
  };

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(jdbc)
        .expect(configuration)
        .expect(ctx)
        .expect(onStop)
        .run(unit -> {
          new jOOQ()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(jdbc)
        .expect(configuration)
        .expect(ctx)
        .expect(onStop)
        .run(unit -> {
          new jOOQ()
              .doWith(c -> assertEquals(unit.get(Configuration.class), c))
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  private Config config() {
    return new jOOQ().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("model"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .resolve();
  }
}
