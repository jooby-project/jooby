package org.jooby.rx;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import rx.Scheduler;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

@SuppressWarnings({"unchecked", "rawtypes" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ExecSchedulerHook.class, Schedulers.class, ImmutableMap.class,
    ImmutableMap.Builder.class })
public class RxJavaSchedulersHookTest {

  private Block scheduler = unit -> {
    unit.mockStatic(Schedulers.class);
    expect(Schedulers.from(unit.get(Executor.class))).andReturn(unit.get(Scheduler.class));
  };

  @Test
  public void ioScheduler() throws Exception {
    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(scheduler)
        .expect(executor("io"))
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(exec("io", unit.get(Executor.class)));
          assertEquals(unit.get(Scheduler.class), hook.getIOScheduler());
        });

    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(noExecutor("io"))
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(Collections.emptyMap());
          assertEquals(null, hook.getIOScheduler());
        });
  }

  private Map<String, Executor> exec(final String name, final Executor executor) {
    Map<String, Executor> map = new HashMap<>();
    map.put(name, executor);
    return map;
  }

  @Test
  public void computationScheduler() throws Exception {
    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(executor("computation"))
        .expect(scheduler)
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(
              exec("computation", unit.get(Executor.class)));
          assertEquals(unit.get(Scheduler.class), hook.getComputationScheduler());
        });

    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(noExecutor("computation"))
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(Collections.emptyMap());
          assertEquals(null, hook.getComputationScheduler());
        });
  }

  @Test
  public void newThreadScheduler() throws Exception {
    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(executor("newThread"))
        .expect(scheduler)
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(
              exec("newThread", unit.get(Executor.class)));
          assertEquals(unit.get(Scheduler.class), hook.getNewThreadScheduler());
        });

    new MockUnit(Map.class, Executor.class, Scheduler.class)
        .expect(noExecutor("newThread"))
        .run(unit -> {
          RxJavaSchedulersHook hook = new ExecSchedulerHook(Collections.emptyMap());
          assertEquals(null, hook.getNewThreadScheduler());
        });
  }

  private Block executor(final String name) {
    return unit -> {
      unit.mockStatic(ImmutableMap.class);

      ImmutableMap map = unit.mock(ImmutableMap.class);
      expect(map.get(name)).andReturn(unit.get(Scheduler.class));

      Builder mb = unit.mock(Builder.class);
      expect(mb.put(name, unit.get(Scheduler.class))).andReturn(mb);

      expect(mb.build()).andReturn(map);

      expect(ImmutableMap.builder()).andReturn(mb);
    };
  }

  private Block noExecutor(final String name) {
    return unit -> {
      unit.mockStatic(ImmutableMap.class);

      ImmutableMap map = unit.mock(ImmutableMap.class);
      expect(map.get(name)).andReturn(null);

      Builder mb = unit.mock(Builder.class);

      expect(mb.build()).andReturn(map);

      expect(ImmutableMap.builder()).andReturn(mb);
    };
  }

}
