package org.jooby.issues;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue482 extends ServerFeature {

  static class Raw {

    String value;

    public Raw(final String value) {
      this.value = value;
    }

  }

  {
    /** Raw JSON renderer .*/
    renderer((value, ctx) -> {
      if (value instanceof String && ctx.accepts(MediaType.json)) {
        ctx.send(value.toString());
      }
    });

    use(new Jackson());

    Executor exec = Executors.newSingleThreadExecutor();

    get("/482/raw", promise(deferred -> {
      exec.execute(deferred.run(() -> Results.json("{\"status\":\"success\"}")));
    }));

  }

  @Test
  public void shouldAcceptResultsFromDeferredAPI() throws Exception {
    request()
        .get("/482/raw")
        .expect("{\"status\":\"success\"}")
        .header("Content-Type", "application/json;charset=UTF-8");
  }

}
