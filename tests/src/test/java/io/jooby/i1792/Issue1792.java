package io.jooby.i1792;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1792 {
  @ServerTest
  public void scriptMvcRouteShouldProducesSameOutput(ServerTestRunner runner) {
    runner.define(app -> {
      app.mvc(new Controller1792());
      app.post("/s/1792", ctx -> StatusCode.CREATED);
    }).ready(client -> {
      client.post("/c/1792", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(StatusCode.CREATED_CODE, rsp.code());
      });

      client.post("/s/1792", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(StatusCode.CREATED_CODE, rsp.code());
      });
    });
  }
}
