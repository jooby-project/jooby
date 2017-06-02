package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class Issue575 extends ServerFeature {

  {
    AtomicInteger inc = new AtomicInteger();
    onStarted(r -> {
      inc.incrementAndGet();
    });

    onStarted(() -> {
      inc.incrementAndGet();
    });

    use((env, conf, binder) -> {
      env.onStarted(r -> inc.incrementAndGet());
      env.onStarted(() -> inc.incrementAndGet());
    });

    get("/575", () -> inc.get());
  }

  @Test
  public void onStartedCallback() throws Exception {
    request()
        .get("/575")
        .expect("4");
  }
}
