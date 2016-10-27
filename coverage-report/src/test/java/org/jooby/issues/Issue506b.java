package org.jooby.issues;

import static org.junit.Assert.assertTrue;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;

public class Issue506b extends ServerFeature {

  {
    executor(MoreExecutors.directExecutor());

    get("/506", deferred(() -> Thread.currentThread().getName()));
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
