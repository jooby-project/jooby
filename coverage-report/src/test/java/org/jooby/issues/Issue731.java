package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.concurrent.Executors;

public class Issue731 extends ServerFeature {

  {

    executor("worker1", Executors.newSingleThreadExecutor());

    post("/", deferred("worker1", req -> req.body(String.class)));

  }

  @Test
  public void appShouldBeAbleToReadTheRequestBodyWhenDeferred() throws Exception {
    request()
            .post("/")
            .body("HelloWorld!", "text/plain")
            .expect(200)
            .expect("HelloWorld!");
  }
}
