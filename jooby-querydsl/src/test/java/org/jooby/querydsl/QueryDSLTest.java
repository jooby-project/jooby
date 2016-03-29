package org.jooby.querydsl;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Env;
import org.jooby.querydsl.internal.ConfigurationProvider;
import org.jooby.querydsl.internal.SQLQueryFactoryProvider;
import org.jooby.querydsl.internal.SQLTemplatesProvider;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import javax.sql.DataSource;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class, H2Templates.class, SQLTemplatesProvider.class})
public class QueryDSLTest {

  @SuppressWarnings("unchecked")
  private MockUnit.Block jdbc = unit -> {
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

    AnnotatedBindingBuilder<SQLTemplates> templatesBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(templatesBinding.toProvider(isA(SQLTemplatesProvider.class))).andReturn(null).times(2);

    AnnotatedBindingBuilder<Configuration> configBinding = unit.mock(AnnotatedBindingBuilder.class);
    configBinding.toInstance(isA(Configuration.class));
    configBinding.toInstance(isA(Configuration.class));
    expectLastCall();

    AnnotatedBindingBuilder<SQLQueryFactory> queryFactoryBinding = unit.mock(AnnotatedBindingBuilder.class);
    expect(queryFactoryBinding.toProvider(SQLQueryFactoryProvider.class)).andReturn(null).times(2);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(SQLTemplates.class))).andReturn(templatesBinding);
    expect(binder.bind(Key.get(SQLTemplates.class, Names.named("db")))).andReturn(templatesBinding);
    expect(binder.bind(Key.get(Configuration.class))).andReturn(configBinding);
    expect(binder.bind(Key.get(Configuration.class, Names.named("db")))).andReturn(configBinding);
    expect(binder.bind(Key.get(SQLQueryFactory.class))).andReturn(queryFactoryBinding);
    expect(binder.bind(Key.get(SQLQueryFactory.class, Names.named("db")))).andReturn(queryFactoryBinding);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Configuration.class)
        .expect(jdbc)
        .expect(configuration)
        .run(unit -> {
          new QueryDSL()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(jdbc)
        .expect(configuration)
        .run(unit -> {
          new QueryDSL()
              .doWith(c -> assertEquals(H2Templates.class, c.getTemplates().getClass()))
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  public Config config() {
    return new QueryDSL().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("model"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .resolve();
  }
}