package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jooby.Jooby;
import org.jooby.netty.Netty;
import org.jooby.test.OnServer;
import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.Test;

@OnServer(Netty.class)
public class Issue421 extends ServerFeature {

  private static CountDownLatch latch = new CountDownLatch(6);

  private static List<String> values = new ArrayList<>();

  public static class Foo extends Jooby {

    {
      onStart(() -> {
        values.add("Start:" + getClass().getSimpleName());
        latch.countDown();
      });
      onStop(() -> {
        values.add("Stop:" + getClass().getSimpleName());
        latch.countDown();
      });

      map(v -> "foo");
    }
  }

  {
    onStart(() -> {
      values.add("Before:" + getClass().getSimpleName());
      latch.countDown();
    });
    onStop(() -> {
      values.add("StopBefore:" + getClass().getSimpleName());
      latch.countDown();
    });

    use(new Foo());

    onStart(() -> {
      values.add("After:" + getClass().getSimpleName());
      latch.countDown();
    });
    onStop(() -> {
      values.add("StopAfter:" + getClass().getSimpleName());
      latch.countDown();
    });

    ScheduledExecutorService exe = Executors.newSingleThreadScheduledExecutor();
    get("/421", () -> {
      exe.schedule(this::stop, 500L, TimeUnit.MILLISECONDS);
      return "bar";
    });
  }

  @Test
  public void shouldImportStartStopCallback() throws Exception {
    request()
        .get("/421")
        .expect("foo");
  }

  @AfterClass
  public static void onStop() throws InterruptedException {
    latch.await();
    assertEquals(
        "[Before:Issue421, Start:Foo, After:Issue421, StopBefore:Issue421, Stop:Foo, StopAfter:Issue421]",
        values.toString());
  }

}
