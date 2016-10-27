package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue506 extends ServerFeature {

  {
    get("/506", deferred("direct", () -> Thread.currentThread().getName()));
  }

  @Test
  public void shouldRunInDirectThread() throws Exception {
    request()
        .get("/506")
        .expect(v -> {
          assertTrue(v.contains("task"));
        });
  }

}
