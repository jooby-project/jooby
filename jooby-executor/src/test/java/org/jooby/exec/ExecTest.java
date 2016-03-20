package org.jooby.exec;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Exec.class, Executors.class, ForkJoinPool.class, Thread.class })
public class ExecTest {

  private Block executors = unit -> {
    unit.mockStatic(Executors.class);
  };

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Runnable.class))).andReturn(env);
  };

  @Test
  public void cached1() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("cached"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(unit -> {
          expect(Executors.newCachedThreadPool(isA(ThreadFactory.class)))
              .andReturn(unit.get(ExecutorService.class));
        })
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void fixed1() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("priority, fixed"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void onStop() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("priority, fixed"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .expect(unit -> {
          ExecutorService es = unit.get(ExecutorService.class);
          es.shutdown();
        })
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          Runnable stop = unit.captured(Runnable.class).iterator().next();
          stop.run();
        });
  }

  @Test
  public void onStopWithFailure() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("priority, fixed"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .expect(unit -> {
          ExecutorService es = unit.get(ExecutorService.class);
          es.shutdown();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));
        })
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        }, unit -> {
          Runnable stop = unit.captured(Runnable.class).iterator().next();
          stop.run();
        });
  }

  @Test
  public void defexec() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          Exec exec = new Exec();
          exec.configure(unit.get(Env.class), exec.config(), unit.get(Binder.class));
        });
  }

  @Test
  public void threadFactory() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    new MockUnit(Env.class, Binder.class, ExecutorService.class, Runnable.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .expect(unit -> {
          Thread t = unit.constructor(Thread.class)
              .args(Runnable.class, String.class)
              .build(unit.get(Runnable.class), "default");
          t.setDaemon(true);
          t.setPriority(Thread.NORM_PRIORITY);
        })
        .run(unit -> {
          Exec exec = new Exec();
          exec.configure(unit.get(Env.class), exec.config(), unit.get(Binder.class));
        }, unit -> {
          ThreadFactory tf = unit.captured(ThreadFactory.class).iterator().next();
          tf.newThread(unit.get(Runnable.class));
        });
  }

  @Test
  public void threadFactoryNotDaemonMaxPriority() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    new MockUnit(Env.class, Binder.class, ExecutorService.class, Runnable.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .expect(unit -> {
          Thread t = unit.constructor(Thread.class)
              .args(Runnable.class, String.class)
              .build(unit.get(Runnable.class), "default");
          t.setDaemon(false);
          t.setPriority(Thread.MAX_PRIORITY);
        })
        .run(unit -> {
          Exec exec = new Exec().daemon(false).priority(Thread.MAX_PRIORITY);
          exec.configure(unit.get(Env.class), exec.config(), unit.get(Binder.class));
        }, unit -> {
          ThreadFactory tf = unit.captured(ThreadFactory.class).iterator().next();
          tf.newThread(unit.get(Runnable.class));
        });
  }

  @Test
  public void scheduled1() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("scheduled"));
    new MockUnit(Env.class, Binder.class, ScheduledExecutorService.class)
        .expect(executors)
        .expect(scheduledPool(n))
        .expect(bind("default", true, ScheduledExecutorService.class, ExecutorService.class,
            Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void forkJoin() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("forkjoin, asyncMode"));
    new MockUnit(Env.class, Binder.class)
        .expect(executors)
        .expect(unit -> {
          ForkJoinPool pool = unit.constructor(ForkJoinPool.class)
              .args(int.class, ForkJoinWorkerThreadFactory.class, UncaughtExceptionHandler.class,
                  boolean.class)
              .build(eq(n), isA(ForkJoinWorkerThreadFactory.class), eq(null), eq(false));
          unit.registerMock(ExecutorService.class, pool);
        })
        .expect(bind("default", true, ExecutorService.class, Executor.class, ForkJoinPool.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void forkJoinAsync() throws Exception {
    int n = 1;
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("forkjoin=1, asyncMode=true"));
    new MockUnit(Env.class, Binder.class)
        .expect(executors)
        .expect(unit -> {
          ForkJoinPool pool = unit.constructor(ForkJoinPool.class)
              .args(int.class, ForkJoinWorkerThreadFactory.class, UncaughtExceptionHandler.class,
                  boolean.class)
              .build(eq(n), isA(ForkJoinWorkerThreadFactory.class), eq(null), eq(true));
          unit.registerMock(ExecutorService.class, pool);
        })
        .expect(bind("default", true, ExecutorService.class, Executor.class, ForkJoinPool.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void scheduled7() throws Exception {
    int n = 7;
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("scheduled=" + n));
    new MockUnit(Env.class, Binder.class, ScheduledExecutorService.class)
        .expect(executors)
        .expect(scheduledPool(n))
        .expect(bind("default", true, ScheduledExecutorService.class, ExecutorService.class,
            Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void fixed5() throws Exception {
    int n = 8;
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("fixed = " + n));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  private Block fixedPool(final int n) {
    return unit -> {
      expect(Executors.newFixedThreadPool(eq(n), unit.capture(ThreadFactory.class)))
          .andReturn(unit.get(ExecutorService.class));
    };
  }

  private Block scheduledPool(final int n) {
    return unit -> {
      expect(Executors.newScheduledThreadPool(eq(n), isA(ThreadFactory.class)))
          .andReturn(unit.get(ScheduledExecutorService.class));
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block bind(final String name, final boolean one, final Class... classes) {
    return unit -> {
      LinkedBindingBuilder eslbb = unit.mock(LinkedBindingBuilder.class);
      eslbb.toInstance(unit.get(classes[0]));
      int times = classes.length + 1;
      expectLastCall().times(one ? times * 2 : times);

      Binder binder = unit.get(Binder.class);
      Function<Class, Key> k = t -> name == null ? Key.get(t) : Key.get(t, Names.named(name));
      // name
      for (Class t : classes) {
        expect(binder.bind(k.apply(t))).andReturn(eslbb);
      }
      expect(binder.bind(k.apply(unit.get(classes[0]).getClass()))).andReturn(eslbb);
      if (one) {
        for (Class t : classes) {
          expect(binder.bind(Key.get(t))).andReturn(eslbb);
        }
        expect(binder.bind(Key.get(unit.get(classes[0]).getClass()))).andReturn(eslbb);
      }
    };
  }

  @Test
  public void daemon() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("fixed, daemon=false"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrontType() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("wrongtype"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void priority() throws Exception {
    int n = Runtime.getRuntime().availableProcessors();
    Config conf = ConfigFactory.empty()
        .withValue("executors", ConfigValueFactory.fromAnyRef("fixed, priority=5"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(n))
        .expect(bind("default", true, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

  @Test
  public void moreExecutors() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("executors.f1", ConfigValueFactory.fromAnyRef("fixed=1"))
        .withValue("executors.f2", ConfigValueFactory.fromAnyRef("fixed=1"));
    new MockUnit(Env.class, Binder.class, ExecutorService.class)
        .expect(executors)
        .expect(fixedPool(1))
        .expect(fixedPool(1))
        .expect(bind("f1", false, ExecutorService.class, Executor.class))
        .expect(bind("f2", false, ExecutorService.class, Executor.class))
        .expect(onStop)
        .run(unit -> {
          new Exec().configure(unit.get(Env.class), conf, unit.get(Binder.class));
        });
  }

}
