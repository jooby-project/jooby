package org.jooby.jdbi;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.funzy.Throwing;
import org.jooby.scope.Providers;
import org.jooby.scope.RequestScoped;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import javax.sql.DataSource;
import java.util.NoSuchElementException;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jdbi3.class, Jdbi.class, Providers.class, OpenHandle.class})
public class Jdbi3Test {

  private MockUnit.Block configure = unit -> {
    DataSource ds = unit.registerMock(DataSource.class);
    Env env = unit.get(Env.class);
    expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.of(ds));

    expect(env.serviceKey()).andReturn(new Env.ServiceKey());

    unit.mockStatic(Jdbi.class);
    Jdbi jdbi = unit.registerMock(Jdbi.class);
    expect(Jdbi.create(ds)).andReturn(jdbi);

    LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
    lbb.toInstance(jdbi);
    lbb.toInstance(jdbi);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(Jdbi.class))).andReturn(lbb);
    expect(binder.bind(Key.get(Jdbi.class, Names.named("db")))).andReturn(lbb);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(configure)
        .run(unit -> {
          new Jdbi3()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void dataSourceMissing() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.get(Key.get(DataSource.class, Names.named("db")))).andReturn(Optional.empty());
        })
        .run(unit -> {
          new Jdbi3()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void configureWithCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, JdbiPlugin.class)
        .expect(configure)
        .expect(unit -> {
          Jdbi jdbi = unit.get(Jdbi.class);
          expect(jdbi.installPlugin(unit.get(JdbiPlugin.class))).andReturn(jdbi);
        })
        .run(unit -> {
          new Jdbi3()
              .doWith(jdbi -> {
                jdbi.installPlugin(unit.get(JdbiPlugin.class));
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void transactionPerRequest() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Handle.class, Registry.class)
        .expect(configure)
        .expect(unit -> {
          Provider<Handle> provider = unit.mock(Provider.class);

          Key<Handle> handleKey = Key.get(Handle.class, Names.named("trx"));
          unit.mockStatic(Providers.class);
          expect(Providers.outOfScope(handleKey)).andReturn(provider);

          LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
          expect(lbb.toProvider(provider)).andReturn(lbb);
          lbb.in(RequestScoped.class);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(handleKey)).andReturn(lbb);
        })
        .expect(unit -> {
          Handle handle = unit.get(Handle.class);
          expect(handle.attach(Jdbi3Test.class)).andReturn(null);

          Registry registry = unit.get(Registry.class);
          Key<Handle> handleKey = Key.get(Handle.class, Names.named("trx"));
          expect(registry.require(handleKey)).andReturn(handle);

          Env env = unit.get(Env.class);
          expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);

          AnnotatedBindingBuilder<Jdbi3Test> lbb = unit.mock(AnnotatedBindingBuilder.class);
          expect(lbb.toProvider(unit.capture(com.google.inject.Provider.class))).andReturn(lbb);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Jdbi3Test.class)).andReturn(lbb);
        })
        .expect(unit -> {
          OpenHandle filter = unit.constructor(OpenHandle.class)
              .args(Jdbi.class, TransactionalRequest.class)
              .build(eq(unit.get(Jdbi.class)), isA(TransactionalRequest.class));

          Route.Definition def = unit.mock(Route.Definition.class);
          expect(def.name("transactionPerRequest")).andReturn(def);

          Router router = unit.mock(Router.class);
          expect(router.use("GET", "/api/**", filter)).andReturn(def);

          Env env = unit.get(Env.class);
          expect(env.router()).andReturn(router);
        })
        .run(unit -> {
          new Jdbi3()
              .transactionPerRequest(new TransactionalRequest()
                  .handle("trx")
                  .pattern("/api/**")
                  .method("GET")
                  .attach(Jdbi3Test.class)
              )
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next().accept(unit.get(Registry.class));
          unit.captured(com.google.inject.Provider.class).iterator().next().get();
        });
  }

  @Test
  public void transactionPerRequestDefaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Handle.class, Registry.class)
        .expect(configure)
        .expect(unit -> {
          Provider<Handle> provider = unit.mock(Provider.class);

          Key<Handle> handleKey = Key.get(Handle.class);
          unit.mockStatic(Providers.class);
          expect(Providers.outOfScope(handleKey)).andReturn(provider);

          LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
          expect(lbb.toProvider(provider)).andReturn(lbb);
          lbb.in(RequestScoped.class);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(handleKey)).andReturn(lbb);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);
        })
        .expect(unit -> {
          OpenHandle filter = unit.constructor(OpenHandle.class)
              .args(Jdbi.class, TransactionalRequest.class)
              .build(eq(unit.get(Jdbi.class)), isA(TransactionalRequest.class));

          Route.Definition def = unit.mock(Route.Definition.class);
          expect(def.name("transactionPerRequest")).andReturn(def);

          Router router = unit.mock(Router.class);
          expect(router.use("*", "*", filter)).andReturn(def);

          Env env = unit.get(Env.class);
          expect(env.router()).andReturn(router);
        })
        .run(unit -> {
          new Jdbi3()
              .transactionPerRequest()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }
}
