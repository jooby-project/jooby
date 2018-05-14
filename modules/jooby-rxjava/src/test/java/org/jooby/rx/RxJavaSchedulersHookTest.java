package org.jooby.rx;

import static org.easymock.EasyMock.expect;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Scheduler;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ExecSchedulerHook.class, Schedulers.class})
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
  }

  private Block executor(final String name) {
    return unit -> {
    };
  }

  private Block noExecutor(final String name) {
    return unit -> {
    };
  }

}
