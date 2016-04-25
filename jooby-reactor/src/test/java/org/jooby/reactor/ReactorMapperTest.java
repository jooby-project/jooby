package org.jooby.reactor;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jooby.Deferred;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Reactor.class, Flux.class, Mono.class })
@SuppressWarnings({"rawtypes", "unchecked" })
public class ReactorMapperTest {

  @Test
  public void flux() throws Exception {
    Object value = new Object();
    Throwable cause = new Throwable();
    new MockUnit()
        .expect(unit -> {
          Flux flux = unit.powerMock(Flux.class);

          expect(flux.consume(unit.capture(Consumer.class), unit.capture(Consumer.class)))
              .andReturn(null);

          unit.registerMock(Flux.class, flux);
        })
        .run(unit -> {
          Deferred deferred = (Deferred) Reactor.reactor().map(unit.get(Flux.class));
          deferred.handler((r, x) -> {
            if (r != null) {
              assertEquals(value, r.get());
            }
            if (x != null) {
              assertEquals(cause, x);
            }
          });
        }, unit -> {
          unit.captured(Consumer.class).get(0).accept(value);
          unit.captured(Consumer.class).get(1).accept(cause);
        });
  }

  @Test
  public void fluxWithScheduler() throws Exception {
    Object value = new Object();
    Throwable cause = new Throwable();
    new MockUnit(Supplier.class, Scheduler.class)
        .expect(unit -> {
          Flux flux = unit.powerMock(Flux.class);

          expect(flux.subscribeOn(unit.get(Scheduler.class))).andReturn(flux);
          expect(flux.consume(unit.capture(Consumer.class), unit.capture(Consumer.class)))
              .andReturn(null);

          unit.registerMock(Flux.class, flux);
        })
        .expect(unit -> {
          Supplier supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Scheduler.class));
        })
        .run(unit -> {
          Deferred deferred = (Deferred) Reactor.reactor(unit.get(Supplier.class))
              .map(unit.get(Flux.class));
          deferred.handler((r, x) -> {
            if (r != null) {
              assertEquals(value, r.get());
            }
            if (x != null) {
              assertEquals(cause, x);
            }
          });
        }, unit -> {
          unit.captured(Consumer.class).get(0).accept(value);
          unit.captured(Consumer.class).get(1).accept(cause);
        });
  }

  @Test
  public void mono() throws Exception {
    Object value = new Object();
    Throwable cause = new Throwable();
    new MockUnit()
        .expect(unit -> {
          Mono flux = unit.powerMock(Mono.class);

          expect(flux.consume(unit.capture(Consumer.class), unit.capture(Consumer.class)))
              .andReturn(null);

          unit.registerMock(Mono.class, flux);
        })
        .run(unit -> {
          Deferred deferred = (Deferred) Reactor.reactor().map(unit.get(Mono.class));
          deferred.handler((r, x) -> {
            if (r != null) {
              assertEquals(value, r.get());
            }
            if (x != null) {
              assertEquals(cause, x);
            }
          });
        }, unit -> {
          unit.captured(Consumer.class).get(0).accept(value);
          unit.captured(Consumer.class).get(1).accept(cause);
        });
  }

  @Test
  public void monoWithSchedulder() throws Exception {
    Object value = new Object();
    Throwable cause = new Throwable();
    new MockUnit(Supplier.class, Scheduler.class)
        .expect(unit -> {
          Mono flux = unit.powerMock(Mono.class);

          expect(flux.subscribeOn(unit.get(Scheduler.class))).andReturn(flux);
          expect(flux.consume(unit.capture(Consumer.class), unit.capture(Consumer.class)))
              .andReturn(null);

          unit.registerMock(Mono.class, flux);
        })
        .expect(unit -> {
          Supplier supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Scheduler.class));
        })
        .run(unit -> {
          Deferred deferred = (Deferred) Reactor.reactor(unit.get(Supplier.class))
              .map(unit.get(Mono.class));
          deferred.handler((r, x) -> {
            if (r != null) {
              assertEquals(value, r.get());
            }
            if (x != null) {
              assertEquals(cause, x);
            }
          });
        }, unit -> {
          unit.captured(Consumer.class).get(0).accept(value);
          unit.captured(Consumer.class).get(1).accept(cause);
        });
  }

  @Test
  public void ignored() throws Exception {
    Object value = new Object();
    new MockUnit()
        .run(unit -> {
          assertEquals(value, Reactor.reactor().map(value));
        });
  }

}
