package org.jooby.rx;

import com.github.davidmoten.rx.jdbc.Database;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RxJdbc.class, Database.class})
public class RxJdbcTest {

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  private Block bind = unit -> {
    Env env = unit.get(Env.class);
    expect(env.serviceKey()).andReturn(new Env.ServiceKey());

    DataSource ds = unit.registerMock(DataSource.class);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

    unit.mockStatic(Database.class);

    Database db = unit.mock(Database.class);
    unit.registerMock(Database.class, db);
    expect(Database.fromDataSource(unit.get(DataSource.class))).andReturn(db);

    LinkedBindingBuilder<Database> lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.toInstance(db);
    lbb.toInstance(db);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(Database.class))).andReturn(lbb);
    expect(binder.bind(Key.get(Database.class, Names.named("db")))).andReturn(lbb);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(bind)
        .expect(onStop)
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void noDataSource() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.empty());
        })
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void withDb() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(bind)
        .expect(onStop)
        .run(unit -> {
          new RxJdbc("db")
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        });
  }

  @Test
  public void onStop() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(bind)
        .expect(onStop)
        .expect(unit -> {
          expect(unit.get(Database.class).close()).andReturn(null);
        })
        .run(unit -> {
          new RxJdbc()
              .configure(unit.get(Env.class), config(), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Runnable.class).iterator().next().run();
        });
  }

  private Config config() {
    return new RxJdbc().config()
        .resolve();
  }

}
