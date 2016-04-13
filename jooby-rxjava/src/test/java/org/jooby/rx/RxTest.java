package org.jooby.rx;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.jooby.Env;
import org.jooby.exec.Exec;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Rx.class, Exec.class, Executors.class, ForkJoinPool.class, Thread.class,
    System.class, RxJavaPlugins.class, Schedulers.class })
public class RxTest {

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
  };

  @Test
  public void configure() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("rx.foo", ConfigValueFactory.fromAnyRef("bar"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(unit -> {
          unit.mockStatic(Schedulers.class);
          Schedulers.shutdown();
        })
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.setProperty("rx.foo", "bar")).andReturn(null);
        })
        .expect(unit -> {
          RxJavaPlugins plugins = unit.mock(RxJavaPlugins.class);
          plugins.registerSchedulersHook(isA(RxJavaSchedulersHook.class));

          unit.mockStatic(RxJavaPlugins.class);
          expect(RxJavaPlugins.getInstance()).andReturn(plugins);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
        })
        .expect(onStop)
        .run(unit -> {
          new Rx().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          unit.captured(Runnable.class).get(1).run();
        });
  }

  @Test
  public void shutdownError() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("rx.foo", ConfigValueFactory.fromAnyRef("bar"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(unit -> {
          unit.mockStatic(Schedulers.class);
          Schedulers.shutdown();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.setProperty("rx.foo", "bar")).andReturn(null);
        })
        .expect(unit -> {
          RxJavaPlugins plugins = unit.mock(RxJavaPlugins.class);
          plugins.registerSchedulersHook(isA(RxJavaSchedulersHook.class));

          unit.mockStatic(RxJavaPlugins.class);
          expect(RxJavaPlugins.getInstance()).andReturn(plugins);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
        })
        .expect(onStop)
        .run(unit -> {
          new Rx().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          unit.captured(Runnable.class).get(1).run();
        });
  }

  @Test
  public void shouldNotBreakOnExistingHook() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("rx.foo", ConfigValueFactory.fromAnyRef("bar"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(unit -> {
          unit.mockStatic(Schedulers.class);
          Schedulers.shutdown();
        })
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.setProperty("rx.foo", "bar")).andReturn(null);
        })
        .expect(unit -> {
          RxJavaPlugins plugins = unit.mock(RxJavaPlugins.class);
          plugins.registerSchedulersHook(isA(RxJavaSchedulersHook.class));
          expectLastCall().andThrow(new IllegalStateException("Hook present"));
          expect(plugins.getSchedulersHook()).andReturn(new ExecSchedulerHook(Collections.emptyMap()));

          unit.mockStatic(RxJavaPlugins.class);
          expect(RxJavaPlugins.getInstance()).andReturn(plugins);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
        })
        .expect(onStop)
        .run(unit -> {
          new Rx().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          unit.captured(Runnable.class).get(1).run();
        });
  }

  @Test(expected = IllegalStateException.class)
  public void shouldBreakOnDiffHook() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("rx.foo", ConfigValueFactory.fromAnyRef("bar"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(unit -> {
          unit.mockStatic(Schedulers.class);
          Schedulers.shutdown();
        })
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.setProperty("rx.foo", "bar")).andReturn(null);
        })
        .expect(unit -> {
          RxJavaPlugins plugins = unit.mock(RxJavaPlugins.class);
          plugins.registerSchedulersHook(isA(RxJavaSchedulersHook.class));
          expectLastCall().andThrow(new IllegalStateException("Hook present"));
          expect(plugins.getSchedulersHook()).andReturn(unit.mock(RxJavaSchedulersHook.class));

          unit.mockStatic(RxJavaPlugins.class);
          expect(RxJavaPlugins.getInstance()).andReturn(plugins);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
        })
        .expect(onStop)
        .run(unit -> {
          new Rx().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          unit.captured(Runnable.class).get(1).run();
        });
  }

  @Test
  public void config() throws Exception {
    assertEquals(ConfigFactory.parseResources(Rx.class, "rx.conf"), new Rx().config());
  }
}
