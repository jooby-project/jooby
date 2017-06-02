package org.jooby.issues;

import java.util.concurrent.Executors;

import org.jooby.assets.AssetsBase;
import org.junit.Test;

public class Issue556 extends AssetsBase {

  {
    executor("async", Executors.newFixedThreadPool(10));

    get("/556/async", deferred("async", () -> "Async"));
  }

  @Test
  public void async() throws Exception {
    request()
        .get("/556/async")
        .expect("Async");
  }

}
