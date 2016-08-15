package org.jooby.scanner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Registry;
import org.jooby.Routes;
import org.jooby.mvc.Path;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;

import app.ns.AbsController;
import app.ns.AbsFoo;
import app.ns.FooApp;
import app.ns.FooController;
import app.ns.FooImpl;
import app.ns.FooModule;
import app.ns.FooSub;
import app.ns.GuavaService;
import app.ns.GuiceModule;
import app.ns.IFoo;
import app.ns.NamedFoo;
import app.ns.SingletonFoo;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import javaslang.control.Try.CheckedConsumer;
import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Scanner.class, FastClasspathScanner.class, FooModule.class, ServiceManager.class })
public class ScannerTest {

  private Block routes = unit -> {
    Env env = unit.get(Env.class);
    expect(env.routes()).andReturn(unit.get(Routes.class));
  };

  private Block runtimeProcessors = unit -> {
    expect(unit.get(Config.class).getInt("runtime.processors")).andReturn(1);
  };

  @Test
  public void newScanner() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void newScannerWithSpec() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(runtimeProcessors)
        .expect(scanResult("test.pkg"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .run(unit -> {
          new Scanner("test.pkg")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanController() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class, FooController.class.getName()))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(unit -> {
          Routes routes = unit.get(Routes.class);
          expect(routes.use(FooController.class)).andReturn(null);
        })
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanModule() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class, FooModule.class.getName()))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          expect(binder.bind(FooModule.class)).andReturn(null);
        })
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanApp() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class, FooApp.class.getName()))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(unit -> {
          Routes routes = unit.get(Routes.class);
          expect(routes.use(isA(FooApp.class))).andReturn(routes);
        })
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void scanAnnotation() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns", "javax.inject", "com.google.inject.name"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(annotations(javax.inject.Named.class, NamedFoo.class.getName()))
        .expect(annotations(com.google.inject.name.Named.class))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<NamedFoo> abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();
          expect(binder.bind(NamedFoo.class)).andReturn(abb);

          Env env = unit.get(Env.class);
          expect(env.lifeCycle(NamedFoo.class)).andReturn(env);
        })
        .run(unit -> {
          new Scanner()
              .scan(javax.inject.Named.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void scanSingleton() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns", "javax.inject", "com.google.inject"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(annotations(javax.inject.Singleton.class, SingletonFoo.class.getName()))
        .expect(annotations(com.google.inject.Singleton.class))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<SingletonFoo> abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();
          expect(binder.bind(SingletonFoo.class)).andReturn(abb);

          Env env = unit.get(Env.class);
          expect(env.lifeCycle(SingletonFoo.class)).andReturn(env);
        })
        .run(unit -> {
          new Scanner()
              .scan(javax.inject.Singleton.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void scanImplements() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(implementing(IFoo.class, FooImpl.class.getName()))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<FooImpl> abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();
          expect(binder.bind(FooImpl.class)).andReturn(abb);

          Env env = unit.get(Env.class);
          expect(env.lifeCycle(FooImpl.class)).andReturn(env);
        })
        .run(unit -> {
          new Scanner()
              .scan(IFoo.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void scanSubclass() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(subClassesOf(AbsFoo.class, FooSub.class.getName()))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          AnnotatedBindingBuilder<FooSub> abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();
          expect(binder.bind(FooSub.class)).andReturn(abb);

          Env env = unit.get(Env.class);
          expect(env.lifeCycle(FooSub.class)).andReturn(env);
        })
        .run(unit -> {
          new Scanner()
              .scan(AbsFoo.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanGuiceModule() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns", "com.google.inject"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(implementing(Module.class, GuiceModule.class.getName()))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          expect(binder.bind(GuiceModule.class)).andReturn(null);
        })
        .run(unit -> {
          new Scanner()
              .scan(Module.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void scanGuavaService() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Registry.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns", "com.google.common.util.concurrent"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(implementing(Service.class, GuavaService.class.getName()))
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);
          AnnotatedBindingBuilder<GuavaService> abb = unit.mock(AnnotatedBindingBuilder.class);
          abb.asEagerSingleton();
          expect(binder.bind(GuavaService.class)).andReturn(abb);

          AnnotatedBindingBuilder<ServiceManager> abbsm = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbsm.toProvider(unit.capture(Provider.class))).andReturn(abbsm);
          expect(binder.bind(ServiceManager.class)).andReturn(abbsm);

          Env env = unit.get(Env.class);
          expect(env.onStart(unit.capture(CheckedConsumer.class))).andReturn(env);
          expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
        })
        .expect(unit -> {
          GuavaService service = unit.mock(GuavaService.class);
          Registry registry = unit.get(Registry.class);
          expect(registry.require(GuavaService.class)).andReturn(service);

          ServiceManager sm = unit.constructor(ServiceManager.class)
              .build(Lists.newArrayList(service));

          unit.registerMock(ServiceManager.class, sm);

          expect(sm.startAsync()).andReturn(sm);
          sm.awaitHealthy();

          expect(sm.stopAsync()).andReturn(sm);
          sm.awaitStopped();
        })
        .run(unit -> {
          new Scanner()
              .scan(Service.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));

          assertEquals(unit.get(ServiceManager.class),
              unit.captured(Provider.class).iterator().next().get());

          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @Test
  public void scanEmptyGuavaService() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class, Registry.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns", "com.google.common.util.concurrent"))
        .expect(routes)
        .expect(annotations(Path.class))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(implementing(Service.class))
        .run(unit -> {
          new Scanner()
              .scan(Service.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanShouldCreateJustOneController() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(
            annotations(Path.class, FooController.class.getName(), FooController.class.getName()))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .expect(unit -> {
          Routes routes = unit.get(Routes.class);
          expect(routes.use(FooController.class)).andReturn(null);
        })
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void scanShouldIgnoreAbsClasses() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Routes.class)
        .expect(ns("app.ns"))
        .expect(runtimeProcessors)
        .expect(scanResult("app.ns"))
        .expect(routes)
        .expect(
            annotations(Path.class, AbsController.class.getName()))
        .expect(implementing(Jooby.Module.class))
        .expect(subClassesOf(Jooby.class))
        .expect(appclass(ScannerTest.class.getName()))
        .run(unit -> {
          new Scanner()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("rawtypes")
  private Block annotations(final Class type, final String... names) {
    return annotations(type, Arrays.asList(names));
  }

  @SuppressWarnings("rawtypes")
  private Block annotations(final Class type, final List<String> classes) {
    return unit -> {
      ScanResult result = unit.get(ScanResult.class);
      expect(result.getNamesOfClassesWithAnnotation(type)).andReturn(classes);
    };
  }

  @SuppressWarnings("rawtypes")
  private Block implementing(final Class type, final String... names) {
    return implementing(type, Arrays.asList(names));
  }

  @SuppressWarnings("rawtypes")
  private Block implementing(final Class type, final List<String> classes) {
    return unit -> {
      ScanResult result = unit.get(ScanResult.class);
      expect(result.getNamesOfClassesImplementing(type))
          .andReturn(classes);
    };
  }

  @SuppressWarnings("rawtypes")
  private Block subClassesOf(final Class type, final String... names) {
    return subClassesOf(type, Arrays.asList(names));
  }

  @SuppressWarnings("rawtypes")
  private Block subClassesOf(final Class type, final List<String> classes) {
    return unit -> {
      ScanResult result = unit.get(ScanResult.class);
      expect(result.getNamesOfSubclassesOf(type))
          .andReturn(classes);
    };
  }

  private Block ns(final String pkg) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getString("application.ns")).andReturn(pkg);
    };
  }

  private Block appclass(final String main) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getString("application.class")).andReturn(main);
    };
  }

  private Block scanResult(final Object... spec) {
    return unit -> {
      FastClasspathScanner scanner = unit.constructor(FastClasspathScanner.class)
          .args(String[].class)
          .build(spec);

      ScanResult result = unit.mock(ScanResult.class);

      expect(scanner.scan(2)).andReturn(result);

      unit.registerMock(ScanResult.class, result);
    };
  }
}
