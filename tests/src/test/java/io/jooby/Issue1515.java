package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1515 {

  @ServerTest
  public void issue1515(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/{lang:[a-z]{2}}/{page:[^.]+}/", ctx -> ctx.pathMap());

      app.get("/{lang:[a-z]{2}}/", ctx -> ctx.pathMap());

    }).ready(client -> {
      client.get("/ar/page/", rsp -> {
        assertEquals("{lang=ar, page=page}", rsp.body().string());
      });

      client.get("/12/page/", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/abc/page/", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/ar/", rsp -> {
        assertEquals("{lang=ar}", rsp.body().string());
      });

      client.get("/12/", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/abc/", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }
}
