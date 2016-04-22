package org.jooby.rx;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.davidmoten.rx.jdbc.Database;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedConsumer;
import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RxJdbc.class, Database.class })
public class RxJdbcTest {

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
  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);

    expect(env.onStop(unit.capture(CheckedConsumer.class))).andReturn(env);
    expect(env.onStop(unit.capture(CheckedConsumer.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  private Block bind = unit -> {
    LinkedBindingBuilder<Database> lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.asEagerSingleton();
    lbb.asEagerSingleton();
    expect(lbb.toProvider(unit.capture(Provider.class))).andReturn(lbb).times(2);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(Database.class))).andReturn(lbb);
    expect(binder.bind(Key.get(Database.class, Names.named("db")))).andReturn(lbb);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(jdbc)
        .expect(bind)
        .expect(onStop)
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStop() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(jdbc)
        .expect(bind)
        .expect(onStop)
        .expect(unit -> {
          Database db = unit.mock(Database.class);
          expect(db.close()).andReturn(db);

          Registry registry = unit.get(Registry.class);
          expect(registry.require(Key.get(Database.class))).andReturn(db);
        })
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  private Config config() {
    return new RxJdbc().config()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef("my.model"))
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.name", ConfigValueFactory.fromAnyRef("model"))
        .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"))
        .resolve();
  }
}
