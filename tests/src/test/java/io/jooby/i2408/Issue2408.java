package io.jooby.i2408;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.WebClient;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2408 {
  @ServerTest
  public void shouldNotIgnoreAnnotationOnParam(ServerTestRunner runner) {
    runner.define(app -> {
      app.mvc(new C2408());
      app.error((ctx, cause, code) -> {
        ctx.send(cause.getMessage());
      });
    }).ready((WebClient http) -> {

      http.get("/2408/nullable?blah=stuff", rsp -> {
        assertEquals("nothing", rsp.body().string());
      });

      http.get("/2408/nonnull", rsp -> {
        assertEquals("Missing value: 'name'", rsp.body().string());
      });

    });
  }
}
