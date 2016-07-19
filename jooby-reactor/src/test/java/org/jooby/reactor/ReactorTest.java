package org.jooby.reactor;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.Request;
import org.jooby.Route;
import org.jooby.Routes;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.typesafe.config.Config;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Reactor.class, Flux.class, Mono.class })
public class ReactorTest {

  private Block map = unit -> {
    Routes routes = unit.get(Routes.class);
    expect(routes.map(unit.capture(Route.Mapper.class))).andReturn(routes);
    Env env = unit.get(Env.class);
    expect(env.routes()).andReturn(routes);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block flux = unit -> {
    Flux flux = unit.powerMock(Flux.class);
    expect(flux.consume(isA(Consumer.class), isA(Consumer.class))).andReturn(null);

    unit.registerMock(Flux.class, flux);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block mono = unit -> {
    Mono mono = unit.powerMock(Mono.class);
    expect(mono.consume(isA(Consumer.class), isA(Consumer.class))).andReturn(null);

    unit.registerMock(Mono.class, mono);
  };

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(map)
        .run(unit -> {
          new Reactor()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void skipMapper() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(map)
        .run(unit -> {
          new Reactor()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          Route.Mapper mapper = unit.captured(Route.Mapper.class).iterator().next();
          Object value = new Object();
          assertEquals(value, mapper.map(value));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void fluxMapper() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Request.class)
        .expect(map)
        .expect(flux)
        .run(unit -> {
          new Reactor()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          Route.Mapper mapper = unit.captured(Route.Mapper.class).iterator().next();
          Deferred deferred = (Deferred) mapper.map(unit.get(Flux.class));
          deferred.handler(unit.get(Request.class), (r, x) -> {
          });
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void fluxMapperWithAdapter() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Scheduler.class,
        Request.class)
            .expect(map)
            .expect(flux)
            .expect(unit -> {
              Flux flux = unit.get(Flux.class);
              expect(flux.publishOn(unit.get(Scheduler.class))).andReturn(flux);
            })
            .run(unit -> {
              new Reactor()
                  .withFlux(f -> f.publishOn(unit.get(Scheduler.class)))
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              Route.Mapper mapper = unit.captured(Route.Mapper.class).iterator().next();
              Deferred deferred = (Deferred) mapper.map(unit.get(Flux.class));
              deferred.handler(unit.get(Request.class), (r, x) -> {
              });
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void mono() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Request.class)
        .expect(map)
        .expect(mono)
        .run(unit -> {
          new Reactor()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          Route.Mapper mapper = unit.captured(Route.Mapper.class).iterator().next();
          Deferred deferred = (Deferred) mapper.map(unit.get(Mono.class));
          deferred.handler(unit.get(Request.class), (r, x) -> {
          });
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void monoMapperWithAdapter() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Scheduler.class,
        Request.class)
            .expect(map)
            .expect(mono)
            .expect(unit -> {
              Mono mono = unit.get(Mono.class);
              expect(mono.publishOn(unit.get(Scheduler.class))).andReturn(mono);
            })
            .run(unit -> {
              new Reactor()
                  .withMono(f -> f.publishOn(unit.get(Scheduler.class)))
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              Route.Mapper mapper = unit.captured(Route.Mapper.class).iterator().next();
              Deferred deferred = (Deferred) mapper.map(unit.get(Mono.class));
              deferred.handler(unit.get(Request.class), (r, x) -> {
              });
            });
  }

}
