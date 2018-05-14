package org.jooby;

import java.util.concurrent.Executors;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HelloAsyncFeature extends ServerFeature {

  {
    executor(Executors.newSingleThreadExecutor());

    Object ierr = new Object();

    renderer((value, ctx) -> {
      if (value == ierr) {
        throw new IllegalStateException("/intentional err");
      }
    });

    get("/hi", deferred(() -> "hi"));

    get("/err/init", deferred(() -> {
      throw new Err(Status.SERVER_ERROR);
    }));

    get("/err/async", promise(deferred -> {
      throw new Err(Status.NOT_FOUND);
    }));

    get("/err/send", deferred(() -> {
      return ierr;
    }));

    get("/:name", deferred(req -> {
      return req.param("name").value();
    }));
  }

  @Test
  public void async() throws Exception {
    request()
        .get("/hi")
        .expect("hi");

    request()
        .get("/bye")
        .expect("bye");

    request()
        .get("/err/init")
        .expect(500);

    request()
        .get("/err/async")
        .expect(404);

    request()
        .get("/err/send")
        .expect(500);
  }

}
