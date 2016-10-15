package org.jooby.metrics;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.function.BiConsumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.internal.metrics.HealthCheckRegistryProvider;
import org.jooby.internal.metrics.MetricRegistryInitializer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

import javaslang.control.Try.CheckedConsumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Metrics.class, MapBinder.class, Multibinder.class })
public class MetricsTest {

  @SuppressWarnings("unchecked")
  private Block bindRegistry = unit -> {
    MetricRegistry registry = unit.get(MetricRegistry.class);

    AnnotatedBindingBuilder<MetricRegistry> mrABB = unit.mock(AnnotatedBindingBuilder.class);
    mrABB.toInstance(registry);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(MetricRegistry.class)).andReturn(mrABB);
  };

  @SuppressWarnings("unchecked")
  private Block bindRegistryInitializer = unit -> {
    AnnotatedBindingBuilder<MetricRegistryInitializer> mrABB = unit
        .mock(AnnotatedBindingBuilder.class);
    mrABB.asEagerSingleton();

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(MetricRegistryInitializer.class)).andReturn(mrABB);
  };

  @SuppressWarnings("unchecked")
  private Block bindHealthCheckRegistry = unit -> {
    ScopedBindingBuilder hcrpSBB = unit.mock(ScopedBindingBuilder.class);
    hcrpSBB.asEagerSingleton();

    AnnotatedBindingBuilder<HealthCheckRegistry> hcrpABB = unit
        .mock(AnnotatedBindingBuilder.class);

    expect(hcrpABB.toProvider(HealthCheckRegistryProvider.class)).andReturn(hcrpSBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(HealthCheckRegistry.class)).andReturn(hcrpABB);
  };

  private Block newRegistry = unit -> {
    MetricRegistry registry = unit.constructor(MetricRegistry.class)
        .build();
    unit.registerMock(MetricRegistry.class, registry);
  };

  private Block mapBinderStatic = unit -> {
    unit.mockStatic(MapBinder.class);
  };

  private Block multibinderStatic = unit -> {
    unit.mockStatic(Multibinder.class);
  };

  private Block routes = unit -> {
    Router routes = unit.mock(Router.class);
    unit.registerMock(Router.class, routes);

    Env env = unit.get(Env.class);
    expect(env.router()).andReturn(routes);
    MetricHandler handler = unit.constructor(MetricHandler.class).build();

    route(unit, routes, "/sys/metrics", handler);

    route(unit, routes, "/sys/metrics/:type", handler);

    route(unit, routes, "/sys/healthcheck",
        unit.constructor(HealthCheckHandler.class).build());

  };

  @SuppressWarnings("unchecked")
  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedConsumer.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void basic() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Jooby.class,
        MetricRegistryInitializer.class)
            .expect(newRegistry)
            .expect(mapBinderStatic)
            .expect(mapbinder(Metric.class, (unit, binder) -> {
            }))
            .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
            }))
            .expect(multibinderStatic)
            .expect(routes)
            .expect(setbinder(Reporter.class, (unit, binder) -> {

            }))
            .expect(bindRegistry)
            .expect(bindRegistryInitializer)
            .expect(bindHealthCheckRegistry)
            .expect(onStop)
            .expect(unit -> {
              MetricRegistryInitializer closer = unit.get(MetricRegistryInitializer.class);
              closer.close();

              Jooby app = unit.get(Jooby.class);
              expect(app.require(MetricRegistryInitializer.class)).andReturn(closer);
            })
            .run(unit -> {
              new Metrics()
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              unit.captured(CheckedConsumer.class).get(0).accept(unit.get(Jooby.class));
            });
  }

  @Test
  public void basicWithExtRegistry() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          MetricRegistry registry = unit.mock(MetricRegistry.class);
          unit.registerMock(MetricRegistry.class, registry);
        })
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          MetricRegistry registry = unit.get(MetricRegistry.class);
          new Metrics(registry)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void request() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(unit -> {
          Router routes = unit.mock(Router.class);
          Env env = unit.get(Env.class);
          expect(env.router()).andReturn(routes);
          MetricHandler handler = unit.constructor(MetricHandler.class).build();

          route(unit, routes, "/sys/metrics", handler);

          route(unit, routes, "/sys/metrics/:type", handler);

          route(unit, routes, "/sys/healthcheck",
              unit.constructor(HealthCheckHandler.class).build());

          route(unit, routes, "*",
              unit.constructor(InstrumentedHandler.class).build());
        })
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          Metrics metrics = new Metrics().request();
          metrics.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void ping() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(unit -> {
          Router routes = unit.get(Router.class);

          route(unit, routes, "/sys/ping", unit.constructor(PingHandler.class).build());
        })
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          Metrics metrics = new Metrics().ping();
          metrics.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void threadDump() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(unit -> {
          Router routes = unit.get(Router.class);

          route(unit, routes, "/sys/thread-dump", unit.constructor(ThreadDumpHandler.class).build());
        })
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          Metrics metrics = new Metrics().threadDump();
          metrics.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void metricInstance() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Metric.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(Metric.class, (unit, binder) -> {
          LinkedBindingBuilder<Metric> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(unit.get(Metric.class));
          expect(binder.addBinding("m")).andReturn(mLBB);
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          new Metrics()
              .metric("m", unit.get(Metric.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void metricRef() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(Metric.class, (unit, binder) -> {
          LinkedBindingBuilder<Metric> mLBB = unit.mock(LinkedBindingBuilder.class);
          expect(mLBB.to(Meter.class)).andReturn(null);
          expect(binder.addBinding("m")).andReturn(mLBB);
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          new Metrics()
              .metric("m", Meter.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void healthCheck() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, HealthCheck.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
          LinkedBindingBuilder<HealthCheck> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(unit.get(HealthCheck.class));
          expect(binder.addBinding("h")).andReturn(mLBB);
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          new Metrics()
              .healthCheck("h", unit.get(HealthCheck.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void healthRef() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
          LinkedBindingBuilder<HealthCheck> mLBB = unit.mock(LinkedBindingBuilder.class);
          expect(mLBB.to(HealthCheck.class)).andReturn(null);
          expect(binder.addBinding("h")).andReturn(mLBB);
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {

        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          new Metrics()
              .healthCheck("h", HealthCheck.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void reporter() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(newRegistry)
        .expect(mapBinderStatic)
        .expect(mapbinder(Metric.class, (unit, binder) -> {
        }))
        .expect(mapbinder(HealthCheck.class, (unit, binder) -> {
        }))
        .expect(multibinderStatic)
        .expect(routes)
        .expect(setbinder(Reporter.class, (unit, binder) -> {
          LinkedBindingBuilder<Reporter> rLBB = unit.mock(LinkedBindingBuilder.class);
          rLBB.toInstance(isA(ConsoleReporter.class));

          expect(binder.addBinding()).andReturn(rLBB);
        }))
        .expect(bindRegistry)
        .expect(bindRegistryInitializer)
        .expect(bindHealthCheckRegistry)
        .expect(onStop)
        .run(unit -> {
          new Metrics()
              .reporter(r -> {
                return ConsoleReporter.forRegistry(r).build();
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private void route(final MockUnit unit, final Router route,
      final String pattern, final Route.Handler handler) throws Exception {
    expect(route.use("GET", pattern, handler)).andReturn(null);
  }

  private void route(final MockUnit unit, final Router route,
      final String pattern, final Route.Filter handler) throws Exception {
    expect(route.use("GET", pattern, handler)).andReturn(null);
  }

  @SuppressWarnings("unchecked")
  private <T> Block mapbinder(final Class<T> type,
      final BiConsumer<MockUnit, MapBinder<String, T>> callback) {
    return unit -> {
      Binder binder = unit.get(Binder.class);
      MapBinder<String, T> m = unit.mock(MapBinder.class);
      expect(MapBinder.newMapBinder(binder, String.class, type)).andReturn(m);
      callback.accept(unit, m);
    };
  }

  @SuppressWarnings("unchecked")
  private <T> Block setbinder(final Class<T> type,
      final BiConsumer<MockUnit, Multibinder<T>> callback) {
    return unit -> {
      Binder binder = unit.get(Binder.class);
      Multibinder<T> m = unit.mock(Multibinder.class);
      expect(Multibinder.newSetBinder(binder, type)).andReturn(m);
      callback.accept(unit, m);
    };
  }

}
