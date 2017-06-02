package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue376 extends ServerFeature {

  {
    before("/before", (req, rsp) -> {
      req.set("counter", 1);
    }, (req, rsp) -> {
      int counter = req.get("counter");
      req.set("counter", counter + 1);
    }, (req, rsp) -> {
      int counter = req.get("counter");
      req.set("counter", counter + 1);
    });

    after("/after", (req, rsp, result) -> {
      int counter = result.get();
      return Results.ok(counter + 1);
    }, (req, rsp, result) -> {
      int counter = result.get();
      return Results.ok(counter + 1);
    });

    AtomicInteger completeCounter = new AtomicInteger(0);

    complete("/complete", (req, rsp, cause) -> {
      completeCounter.incrementAndGet();
    }, (req, rsp, cause) -> {
      completeCounter.incrementAndGet();
    }, (req, rsp, cause) -> {
      completeCounter.incrementAndGet();
    });

    get("/before", req -> req.get("counter"));

    get("/after", req -> 1);

    get("/complete", req -> 1);

    onStop(() -> {
      assertEquals(3, completeCounter.get());
    });
  }

  @Test
  public void multipleBeforeFilter() throws Exception {
    request()
        .get("/before")
        .expect("3");
  }

  @Test
  public void multipleAfterFilter() throws Exception {
    request()
        .get("/after")
        .expect("3");
  }

  @Test
  public void multipleCompleteFilter() throws Exception {
    request()
        .get("/complete")
        .expect("1");
  }

}
